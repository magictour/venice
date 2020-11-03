package com.linkedin.venice.compute;

import com.linkedin.venice.compute.protocol.request.ComputeOperation;
import com.linkedin.venice.compute.protocol.request.Count;
import com.linkedin.venice.exceptions.VeniceException;
import com.linkedin.venice.listener.response.ComputeResponseWrapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.avro.generic.GenericRecord;


public class CountOperator implements ReadComputeOperator {
  @Override
  public void compute(int computeRequestVersion, ComputeOperation op, GenericRecord valueRecord, GenericRecord resultRecord,
      Map<String, String> computationErrorMap, Map<String, Object> context, ComputeResponseWrapper responseWrapper) {
    responseWrapper.incrementCountOperatorCount();
    Count count = (Count) op.operation;
    String resultFieldName = count.resultFieldName.toString();
    try {
      Object o = valueRecord.get(count.field.toString());
      if (o instanceof Map) {
        Map map = (Map)o;
        putResult(resultRecord, resultFieldName,  map.size());
      } else if (o instanceof Collection) {
        Collection collection = (Collection)o;
        putResult(resultRecord, resultFieldName, collection.size());
      } else {
        throw new VeniceException("Record field " + resultFieldName + " is not valid for count operation, only Map/Array are supported.");
      }
    } catch (Exception e) {
      putResult(resultRecord, resultFieldName, -1);
      String msg = e.getClass().getSimpleName() + " : " + (e.getMessage() == null ? "Failed to execute count operator." : e.getMessage());
      computationErrorMap.put(resultFieldName, msg);
    }
  }

  public String getOperatorFieldName(ComputeOperation op) {
    Count operation = (Count) op.operation;
    return operation.field.toString();
  }

  public String getResultFieldName(ComputeOperation op) {
    Count operation = (Count) op.operation;
    return operation.resultFieldName.toString();
  }
  @Override
  public void putDefaultResult(GenericRecord record, String field) {
    putResult(record, field, 0);
  }
}
