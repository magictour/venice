package com.linkedin.venice.hadoop;

import static com.linkedin.venice.hadoop.VenicePushJob.STORAGE_ENGINE_OVERHEAD_RATIO;
import static com.linkedin.venice.hadoop.VenicePushJob.STORAGE_QUOTA_PROP;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.linkedin.venice.exceptions.UndefinedPropertyException;
import com.linkedin.venice.exceptions.VeniceException;
import com.linkedin.venice.utils.VeniceProperties;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.mapred.AvroWrapper;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.Counters;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestVeniceAvroMapper extends AbstractTestVeniceMapper<VeniceAvroMapper> {
  protected VeniceAvroMapper newMapper() {
    return new VeniceAvroMapper();
  }

  @Test(dataProvider = MAPPER_PARAMS_DATA_PROVIDER)
  public void testConfigure(int numReducers, int taskId) {
    JobConf job = setupJobConf(numReducers, taskId);
    VeniceAvroMapper mapper = new VeniceAvroMapper();
    try {
      mapper.configure(job);
    } catch (Exception e) {
      Assert.fail("VeniceAvroMapper#configure should not throw any exception when all the required props are there");
    }
  }

  @Test(expectedExceptions = UndefinedPropertyException.class)
  public void testConfigureWithMissingProps() {
    JobConf job = setupJobConf();
    job.unset(VenicePushJob.TOPIC_PROP);
    VeniceAvroMapper mapper = new VeniceAvroMapper();
    mapper.configure(job);
  }

  @Test(dataProvider = MAPPER_PARAMS_DATA_PROVIDER)
  public void testMap(int numReducers, int taskId) throws IOException {
    final String keyFieldValue = "key_field_value";
    final String valueFieldValue = "value_field_value";
    AvroWrapper<IndexedRecord> wrapper = getAvroWrapper(keyFieldValue, valueFieldValue);
    OutputCollector<BytesWritable, BytesWritable> output = mock(OutputCollector.class);

    VeniceAvroMapper mapper = getMapper(numReducers, taskId);
    mapper.map(wrapper, NullWritable.get(), output, null);

    ArgumentCaptor<BytesWritable> keyCaptor = ArgumentCaptor.forClass(BytesWritable.class);
    ArgumentCaptor<BytesWritable> valueCaptor = ArgumentCaptor.forClass(BytesWritable.class);

    verify(output, times(getNumberOfCollectorInvocationForFirstMapInvocation(numReducers, taskId)))
        .collect(keyCaptor.capture(), valueCaptor.capture());
    Assert.assertTrue(getHexString(keyCaptor.getValue().copyBytes()).endsWith(getHexString(keyFieldValue.getBytes())));
    Assert.assertTrue(
        getHexString(valueCaptor.getValue().copyBytes()).endsWith(getHexString(valueFieldValue.getBytes())));

    /** Subsequent calls should trigger just one call to the {@link OutputCollector}, no matter the task ID */
    final String keyFieldValue2 = "key_field_value_2";
    final String valueFieldValue2 = "value_field_value_2";
    AvroWrapper<IndexedRecord> wrapper2 = getAvroWrapper(keyFieldValue2, valueFieldValue2);
    OutputCollector<BytesWritable, BytesWritable> output2 = mock(OutputCollector.class);

    mapper.map(wrapper2, NullWritable.get(), output2, null);
    verify(output2).collect(keyCaptor.capture(), valueCaptor.capture());
    Assert.assertTrue(getHexString(keyCaptor.getValue().copyBytes()).endsWith(getHexString(keyFieldValue2.getBytes())));
    Assert.assertTrue(
        getHexString(valueCaptor.getValue().copyBytes()).endsWith(getHexString(valueFieldValue2.getBytes())));
  }

  @Test(expectedExceptions = VeniceException.class, dataProvider = MAPPER_PARAMS_DATA_PROVIDER)
  public void testMapWithNullKey(int numReducers, int taskId) throws IOException {
    final String valueFieldValue = "value_field_value";
    AvroWrapper<IndexedRecord> wrapper = getAvroWrapper(null, valueFieldValue);
    OutputCollector<BytesWritable, BytesWritable> output = mock(OutputCollector.class);

    VeniceAvroMapper mapper = getMapper(numReducers, taskId);
    mapper.map(wrapper, NullWritable.get(), output, null);
  }

  @Test(dataProvider = MAPPER_PARAMS_DATA_PROVIDER)
  public void testMapWithNullValue(int numReducers, int taskId) throws IOException {
    final String keyFieldValue = "key_field_value";
    AvroWrapper<IndexedRecord> wrapper = getAvroWrapper(keyFieldValue, null);
    OutputCollector<BytesWritable, BytesWritable> output = mock(OutputCollector.class);

    VeniceAvroMapper mapper = getMapper(numReducers, taskId);
    mapper.map(wrapper, NullWritable.get(), output, mock(Reporter.class));

    verify(output, times(getNumberOfCollectorInvocationForFirstMapInvocation(numReducers, taskId) - 1))
        .collect(Mockito.any(), Mockito.any());
  }

  @Test(dataProvider = MAPPER_PARAMS_DATA_PROVIDER)
  public void testMapWithExceededQuota(int numReducers, int taskId) throws IOException {
    final String keyFieldValue = "key_field_value";
    final String valueFieldValue = "value_field_value";
    AvroWrapper<IndexedRecord> wrapper = getAvroWrapper(keyFieldValue, valueFieldValue);
    OutputCollector<BytesWritable, BytesWritable> output = mock(OutputCollector.class);
    Reporter mockReporter = createMockReporterWithCount(1L);

    VeniceAvroMapper mapper = getMapper(numReducers, taskId, mapperJobConfig -> {
      mapperJobConfig.set(STORAGE_QUOTA_PROP, "18"); // With this setup, storage quota should be exceeded
      mapperJobConfig.set(STORAGE_ENGINE_OVERHEAD_RATIO, "1.5");
    });
    mapper.map(wrapper, NullWritable.get(), output, mockReporter);

    verify(mockReporter, times(1)).incrCounter(
        eq(MRJobCounterHelper.TOTAL_UNCOMPRESSED_VALUE_SIZE_GROUP_COUNTER_NAME.getGroupName()),
        eq(MRJobCounterHelper.TOTAL_UNCOMPRESSED_VALUE_SIZE_GROUP_COUNTER_NAME.getCounterName()),
        eq(18L));

    // Expect no authorization error
    verify(mockReporter, never()).incrCounter(
        eq(MRJobCounterHelper.WRITE_ACL_FAILURE_GROUP_COUNTER_NAME.getGroupName()),
        eq(MRJobCounterHelper.WRITE_ACL_FAILURE_GROUP_COUNTER_NAME.getCounterName()),
        anyLong());
    // Even with exceeded quota, the mapper should still count key and value sizes in bytes and output
    verify(mockReporter, times(1)).incrCounter(
        eq(MRJobCounterHelper.TOTAL_KEY_SIZE_GROUP_COUNTER_NAME.getGroupName()),
        eq(MRJobCounterHelper.TOTAL_KEY_SIZE_GROUP_COUNTER_NAME.getCounterName()),
        anyLong());
    verify(mockReporter, times(1)).incrCounter(
        eq(MRJobCounterHelper.TOTAL_VALUE_SIZE_GROUP_COUNTER_NAME.getGroupName()),
        eq(MRJobCounterHelper.TOTAL_VALUE_SIZE_GROUP_COUNTER_NAME.getCounterName()),
        anyLong());
    verify(output, times(getNumberOfCollectorInvocationForFirstMapInvocation(numReducers, taskId)))
        .collect(any(), any());
  }

  @Test(dataProvider = MAPPER_PARAMS_DATA_PROVIDER)
  public void testMapWithQuotaNotExceeded(int numReducers, int taskId) throws IOException {
    final String keyFieldValue = "key_field_value";
    final String valueFieldValue = "value_field_value";
    AvroWrapper<IndexedRecord> wrapper = getAvroWrapper(keyFieldValue, valueFieldValue);
    OutputCollector<BytesWritable, BytesWritable> output = mock(OutputCollector.class);
    Reporter mockReporter = createMockReporterWithCount(1L);

    VeniceAvroMapper mapper = getMapper(numReducers, taskId, mapperJobConfig -> {
      mapperJobConfig.set(STORAGE_QUOTA_PROP, "100"); // With this setup, storage quota should not exceeded
      mapperJobConfig.set(STORAGE_ENGINE_OVERHEAD_RATIO, "1.5");
    });
    mapper.map(wrapper, NullWritable.get(), output, mockReporter);

    verify(mockReporter, times(1)).incrCounter(
        eq(MRJobCounterHelper.TOTAL_UNCOMPRESSED_VALUE_SIZE_GROUP_COUNTER_NAME.getGroupName()),
        eq(MRJobCounterHelper.TOTAL_UNCOMPRESSED_VALUE_SIZE_GROUP_COUNTER_NAME.getCounterName()),
        eq(18L));

    // Not write ACL failure
    verify(mockReporter, never()).incrCounter(
        eq(MRJobCounterHelper.WRITE_ACL_FAILURE_GROUP_COUNTER_NAME.getGroupName()),
        eq(MRJobCounterHelper.WRITE_ACL_FAILURE_GROUP_COUNTER_NAME.getCounterName()),
        anyLong());
    // Expect reporter to record these counters due to no early termination
    verify(mockReporter, times(1)).incrCounter(
        eq(MRJobCounterHelper.TOTAL_KEY_SIZE_GROUP_COUNTER_NAME.getGroupName()),
        eq(MRJobCounterHelper.TOTAL_KEY_SIZE_GROUP_COUNTER_NAME.getCounterName()),
        anyLong());
    verify(mockReporter, times(1)).incrCounter(
        eq(MRJobCounterHelper.TOTAL_VALUE_SIZE_GROUP_COUNTER_NAME.getGroupName()),
        eq(MRJobCounterHelper.TOTAL_VALUE_SIZE_GROUP_COUNTER_NAME.getCounterName()),
        anyLong());
    // Expect the output collect to collect output due to no early termination
    verify(output, times(getNumberOfCollectorInvocationForFirstMapInvocation(numReducers, taskId)))
        .collect(any(), any());
  }

  @Test
  public void testEmptyFilter() {
    try (VeniceAvroMapper mapper = new VeniceAvroMapper()) {
      Assert.assertFalse(mapper.getFilter(new VeniceProperties()).isPresent());
    }
  }

  private AvroWrapper<IndexedRecord> getAvroWrapper(String keyFieldValue, String valueFieldValue) {
    GenericData.Record record = new GenericData.Record(Schema.parse(SCHEMA_STR));
    record.put(KEY_FIELD, keyFieldValue);
    record.put(VALUE_FIELD, valueFieldValue);
    return new AvroWrapper<>(record);
  }

  private Reporter createMockReporterWithCount(long countToReturn) {
    Reporter mockReporter = mock(Reporter.class);
    Counters.Counter mockCounter = mock(Counters.Counter.class);
    when(mockCounter.getCounter()).thenReturn(countToReturn);
    when(mockReporter.getCounter(anyString(), anyString())).thenReturn(mockCounter);
    return mockReporter;
  }
}
