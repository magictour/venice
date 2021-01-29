/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package com.linkedin.venice.systemstore.schemas;

@SuppressWarnings("all")
/** Type describes all the version attributes */
public class StoreVersion extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = org.apache.avro.Schema.parse("{\"type\":\"record\",\"name\":\"StoreVersion\",\"namespace\":\"com.linkedin.venice.systemstore.schemas\",\"fields\":[{\"name\":\"storeName\",\"type\":\"string\",\"doc\":\"Name of the store which this version belong to.\"},{\"name\":\"number\",\"type\":\"int\",\"doc\":\"Version number.\"},{\"name\":\"createdTime\",\"type\":\"long\",\"doc\":\"Time when this version was created.\"},{\"name\":\"status\",\"type\":\"int\",\"doc\":\"Status of version, and default is 'STARTED'\",\"default\":1},{\"name\":\"pushJobId\",\"type\":\"string\",\"default\":\"\"},{\"name\":\"compressionStrategy\",\"type\":\"int\",\"doc\":\"strategies used to compress/decompress Record's value, and default is 'NO_OP'\",\"default\":0},{\"name\":\"leaderFollowerModelEnabled\",\"type\":\"boolean\",\"doc\":\"Whether or not to use leader follower state transition.\",\"default\":false},{\"name\":\"nativeReplicationEnabled\",\"type\":\"boolean\",\"doc\":\"Whether or not native replication is enabled.\",\"default\":false},{\"name\":\"pushStreamSourceAddress\",\"type\":\"string\",\"doc\":\"Address to the kafka broker which holds the source of truth topic for this store version.\",\"default\":\"\"},{\"name\":\"bufferReplayEnabledForHybrid\",\"type\":\"boolean\",\"doc\":\"Whether or not to enable buffer replay for hybrid.\",\"default\":true},{\"name\":\"chunkingEnabled\",\"type\":\"boolean\",\"doc\":\"Whether or not large values are supported (via chunking).\",\"default\":false},{\"name\":\"pushType\",\"type\":\"int\",\"doc\":\"Producer type for this version, and default is 'BATCH'\",\"default\":0},{\"name\":\"partitionCount\",\"type\":\"int\",\"doc\":\"Partition count of this version.\",\"default\":0},{\"name\":\"partitionerConfig\",\"type\":[\"null\",{\"type\":\"record\",\"name\":\"StorePartitionerConfig\",\"fields\":[{\"name\":\"partitionerClass\",\"type\":\"string\"},{\"name\":\"partitionerParams\",\"type\":{\"type\":\"map\",\"values\":\"string\"}},{\"name\":\"amplificationFactor\",\"type\":\"int\"}]}],\"doc\":\"Config for custom partitioning.\",\"default\":null},{\"name\":\"incrementalPushPolicy\",\"type\":\"int\",\"doc\":\"Incremental Push Policy to reconcile with real time pushes., and default is 'PUSH_TO_VERSION_TOPIC'\",\"default\":0},{\"name\":\"replicationFactor\",\"type\":\"int\",\"doc\":\"The number of replica this store version is keeping.\",\"default\":3},{\"name\":\"nativeReplicationSourceFabric\",\"type\":\"string\",\"doc\":\"The source fabric name to be uses in native replication. Remote consumption will happen from kafka in this fabric.\",\"default\":\"\"},{\"name\":\"incrementalPushEnabled\",\"type\":\"boolean\",\"doc\":\"Flag to see if the store supports incremental push or not\",\"default\":false},{\"name\":\"useVersionLevelIncrementalPushEnabled\",\"type\":\"boolean\",\"doc\":\"Flag to see if incrementalPushEnabled config at StoreVersion should be used. This is needed during migration of this config from Store level to Version level. We can deprecate this field later.\",\"default\":false},{\"name\":\"hybridConfig\",\"type\":[\"null\",{\"type\":\"record\",\"name\":\"StoreHybridConfig\",\"fields\":[{\"name\":\"rewindTimeInSeconds\",\"type\":\"long\"},{\"name\":\"offsetLagThresholdToGoOnline\",\"type\":\"long\"},{\"name\":\"producerTimestampLagThresholdToGoOnlineInSeconds\",\"type\":\"long\"}]}],\"doc\":\"Properties related to Hybrid Store behavior. If absent (null), then the store is not hybrid.\",\"default\":null},{\"name\":\"useVersionLevelHybridConfig\",\"type\":\"boolean\",\"doc\":\"Flag to see if hybridConfig at StoreVersion should be used. This is needed during migration of this config from Store level to Version level. We can deprecate this field later.\",\"default\":false}]}");
  /** Name of the store which this version belong to. */
  public java.lang.CharSequence storeName;
  /** Version number. */
  public int number;
  /** Time when this version was created. */
  public long createdTime;
  /** Status of version, and default is 'STARTED' */
  public int status;
  public java.lang.CharSequence pushJobId;
  /** strategies used to compress/decompress Record's value, and default is 'NO_OP' */
  public int compressionStrategy;
  /** Whether or not to use leader follower state transition. */
  public boolean leaderFollowerModelEnabled;
  /** Whether or not native replication is enabled. */
  public boolean nativeReplicationEnabled;
  /** Address to the kafka broker which holds the source of truth topic for this store version. */
  public java.lang.CharSequence pushStreamSourceAddress;
  /** Whether or not to enable buffer replay for hybrid. */
  public boolean bufferReplayEnabledForHybrid;
  /** Whether or not large values are supported (via chunking). */
  public boolean chunkingEnabled;
  /** Producer type for this version, and default is 'BATCH' */
  public int pushType;
  /** Partition count of this version. */
  public int partitionCount;
  /** Config for custom partitioning. */
  public com.linkedin.venice.systemstore.schemas.StorePartitionerConfig partitionerConfig;
  /** Incremental Push Policy to reconcile with real time pushes., and default is 'PUSH_TO_VERSION_TOPIC' */
  public int incrementalPushPolicy;
  /** The number of replica this store version is keeping. */
  public int replicationFactor;
  /** The source fabric name to be uses in native replication. Remote consumption will happen from kafka in this fabric. */
  public java.lang.CharSequence nativeReplicationSourceFabric;
  /** Flag to see if the store supports incremental push or not */
  public boolean incrementalPushEnabled;
  /** Flag to see if incrementalPushEnabled config at StoreVersion should be used. This is needed during migration of this config from Store level to Version level. We can deprecate this field later. */
  public boolean useVersionLevelIncrementalPushEnabled;
  /** Properties related to Hybrid Store behavior. If absent (null), then the store is not hybrid. */
  public com.linkedin.venice.systemstore.schemas.StoreHybridConfig hybridConfig;
  /** Flag to see if hybridConfig at StoreVersion should be used. This is needed during migration of this config from Store level to Version level. We can deprecate this field later. */
  public boolean useVersionLevelHybridConfig;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return storeName;
    case 1: return number;
    case 2: return createdTime;
    case 3: return status;
    case 4: return pushJobId;
    case 5: return compressionStrategy;
    case 6: return leaderFollowerModelEnabled;
    case 7: return nativeReplicationEnabled;
    case 8: return pushStreamSourceAddress;
    case 9: return bufferReplayEnabledForHybrid;
    case 10: return chunkingEnabled;
    case 11: return pushType;
    case 12: return partitionCount;
    case 13: return partitionerConfig;
    case 14: return incrementalPushPolicy;
    case 15: return replicationFactor;
    case 16: return nativeReplicationSourceFabric;
    case 17: return incrementalPushEnabled;
    case 18: return useVersionLevelIncrementalPushEnabled;
    case 19: return hybridConfig;
    case 20: return useVersionLevelHybridConfig;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: storeName = (java.lang.CharSequence)value$; break;
    case 1: number = (java.lang.Integer)value$; break;
    case 2: createdTime = (java.lang.Long)value$; break;
    case 3: status = (java.lang.Integer)value$; break;
    case 4: pushJobId = (java.lang.CharSequence)value$; break;
    case 5: compressionStrategy = (java.lang.Integer)value$; break;
    case 6: leaderFollowerModelEnabled = (java.lang.Boolean)value$; break;
    case 7: nativeReplicationEnabled = (java.lang.Boolean)value$; break;
    case 8: pushStreamSourceAddress = (java.lang.CharSequence)value$; break;
    case 9: bufferReplayEnabledForHybrid = (java.lang.Boolean)value$; break;
    case 10: chunkingEnabled = (java.lang.Boolean)value$; break;
    case 11: pushType = (java.lang.Integer)value$; break;
    case 12: partitionCount = (java.lang.Integer)value$; break;
    case 13: partitionerConfig = (com.linkedin.venice.systemstore.schemas.StorePartitionerConfig)value$; break;
    case 14: incrementalPushPolicy = (java.lang.Integer)value$; break;
    case 15: replicationFactor = (java.lang.Integer)value$; break;
    case 16: nativeReplicationSourceFabric = (java.lang.CharSequence)value$; break;
    case 17: incrementalPushEnabled = (java.lang.Boolean)value$; break;
    case 18: useVersionLevelIncrementalPushEnabled = (java.lang.Boolean)value$; break;
    case 19: hybridConfig = (com.linkedin.venice.systemstore.schemas.StoreHybridConfig)value$; break;
    case 20: useVersionLevelHybridConfig = (java.lang.Boolean)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
}
