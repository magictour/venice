package com.linkedin.davinci.store.rocksdb;

import com.linkedin.davinci.stats.RocksDBMemoryStats;
import com.linkedin.davinci.store.StoragePartitionConfig;
import com.linkedin.venice.exceptions.VeniceException;
import com.linkedin.venice.store.rocksdb.RocksDBUtils;
import com.linkedin.venice.utils.ByteUtils;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;


/**
 * This {@link ReplicationMetadataRocksDBStoragePartition} is built to store key value pair along with the timestamp
 * metadata. It is designed for active/active replication mode, which uses putWithReplicationMetadata and getReplicationMetadata
 * to insert and retrieve replication metadata associated with a key. The implementation relies on different column family
 * in RocksDB to isolate the value and replication metadata of a key.
 */
public class ReplicationMetadataRocksDBStoragePartition extends RocksDBStoragePartition {
  private static final Logger LOGGER = LogManager.getLogger(ReplicationMetadataRocksDBStoragePartition.class);

  // The value still uses "timestamp" for backward compatibility
  private RocksDBSstFileWriter rocksDBSstFileWriter = null;
  private final String fullPathForTempSSTFileDir;

  private static final int DEFAULT_COLUMN_FAMILY_INDEX = 0;
  private static final int REPLICATION_METADATA_COLUMN_FAMILY_INDEX = 1;

  public ReplicationMetadataRocksDBStoragePartition(
      StoragePartitionConfig storagePartitionConfig,
      RocksDBStorageEngineFactory factory,
      String dbDir,
      RocksDBMemoryStats rocksDBMemoryStats,
      RocksDBThrottler rocksDbThrottler,
      RocksDBServerConfig rocksDBServerConfig) {
    super(
        storagePartitionConfig,
        factory,
        dbDir,
        rocksDBMemoryStats,
        rocksDbThrottler,
        rocksDBServerConfig,
        Arrays.asList(RocksDB.DEFAULT_COLUMN_FAMILY, REPLICATION_METADATA_COLUMN_FAMILY));
    this.fullPathForTempSSTFileDir = RocksDBUtils.composeTempRMDSSTFileDir(dbDir, storeName, partitionId);
    if (deferredWrite) {
      this.rocksDBSstFileWriter = new RocksDBSstFileWriter(
          storeName,
          partitionId,
          dbDir,
          super.getEnvOptions(),
          super.getOptions(),
          fullPathForTempSSTFileDir,
          true,
          rocksDBServerConfig);
    }
  }

  @Override
  public synchronized void putWithReplicationMetadata(byte[] key, byte[] value, byte[] metadata) {
    makeSureRocksDBIsStillOpen();
    if (readOnly) {
      throw new VeniceException(
          "Cannot make writes while partition is opened in read-only mode" + ", partition=" + storeName + "_"
              + partitionId);
    }

    try {
      if (deferredWrite) {
        super.put(key, value);
        rocksDBSstFileWriter.put(key, ByteBuffer.wrap(metadata));
      } else {
        try (WriteBatch writeBatch = new WriteBatch()) {
          writeBatch.put(columnFamilyHandleList.get(DEFAULT_COLUMN_FAMILY_INDEX), key, value);
          writeBatch.put(columnFamilyHandleList.get(REPLICATION_METADATA_COLUMN_FAMILY_INDEX), key, metadata);
          rocksDB.write(writeOptions, writeBatch);
        }
      }
    } catch (RocksDBException e) {
      throw new VeniceException(
          "Failed to put key/value pair to store: " + storeName + ", partition id: " + partitionId,
          e);
    }
  }

  public long getRmdByteUsage() {
    readCloseRWLock.readLock().lock();
    try {
      makeSureRocksDBIsStillOpen();
      return rocksDB.getColumnFamilyMetaData(columnFamilyHandleList.get(REPLICATION_METADATA_COLUMN_FAMILY_INDEX))
          .size();
    } finally {
      readCloseRWLock.readLock().unlock();
    }
  }

  /**
   * This API takes in value and metadata as ByteBuffer format and put it into RocksDB.
   * Note that it is not an efficient implementation as it copies the content to perform the ByteBuffer -> byte[] conversion.
   * TODO: Rewrite this implementation after we adopt the thread-local direct bytebuffer approach.
   */
  @Override
  public synchronized void putWithReplicationMetadata(byte[] key, ByteBuffer value, byte[] metadata) {
    byte[] valueBytes = ByteUtils.extractByteArray(value);
    putWithReplicationMetadata(key, valueBytes, metadata);
  }

  @Override
  public byte[] getReplicationMetadata(byte[] key) {
    readCloseRWLock.readLock().lock();
    try {
      makeSureRocksDBIsStillOpen();
      return rocksDB
          .get(columnFamilyHandleList.get(REPLICATION_METADATA_COLUMN_FAMILY_INDEX), READ_OPTIONS_DEFAULT, key);
    } catch (RocksDBException e) {
      throw new VeniceException("Failed to get value from store: " + storeName + ", partition id: " + partitionId, e);
    } finally {
      readCloseRWLock.readLock().unlock();
    }
  }

  /**
   * This API deletes a record from RocksDB but updates the metadata in ByteBuffer format and puts it into RocksDB.
   */
  @Override
  public synchronized void deleteWithReplicationMetadata(byte[] key, byte[] replicationMetadata) {
    makeSureRocksDBIsStillOpen();
    if (readOnly) {
      throw new VeniceException(
          "Cannot make writes while partition is opened in read-only mode" + ", partition=" + storeName + "_"
              + partitionId);
    }
    try {
      if (deferredWrite) {
        // Just update the RMD for deletion during repush
        rocksDBSstFileWriter.put(key, ByteBuffer.wrap(replicationMetadata));
      } else {
        try (WriteBatch writeBatch = new WriteBatch()) {
          writeBatch.delete(columnFamilyHandleList.get(DEFAULT_COLUMN_FAMILY_INDEX), key);
          writeBatch
              .put(columnFamilyHandleList.get(REPLICATION_METADATA_COLUMN_FAMILY_INDEX), key, replicationMetadata);
          rocksDB.write(writeOptions, writeBatch);
        }
      }
    } catch (RocksDBException e) {
      String msg = deferredWrite
          ? "Failed to put metadata while deleing key for store: " + storeName + ", partition id: " + partitionId
          : "Failed to delete entry to store: " + storeName + ", partition id: " + partitionId;
      throw new VeniceException(msg, e);
    }
  }

  @Override
  public synchronized void beginBatchWrite(
      Map<String, String> checkpointedInfo,
      Optional<Supplier<byte[]>> expectedChecksumSupplier) {
    if (!deferredWrite) {
      LOGGER.info("'beginBatchWrite' will do nothing since 'deferredWrite' is disabled");
      return;
    }
    super.beginBatchWrite(checkpointedInfo, expectedChecksumSupplier);
    rocksDBSstFileWriter.open(checkpointedInfo, expectedChecksumSupplier);
  }

  @Override
  public synchronized void endBatchWrite() {
    super.endBatchWrite();

    if (deferredWrite) {
      rocksDBSstFileWriter.ingestSSTFiles(rocksDB, getColumnFamilyHandleList());
    }
  }

  @Override
  public synchronized Map<String, String> sync() {
    Map<String, String> checkpointingInfo = super.sync();
    // if deferredWrite is false, super.sync will flush both the column families
    if (deferredWrite) {
      checkpointingInfo.putAll(rocksDBSstFileWriter.sync());
    }
    return checkpointingInfo;
  }

  @Override
  public synchronized void close() {
    super.close();
    if (deferredWrite) {
      rocksDBSstFileWriter.close();
    }
  }

  @Override
  public synchronized boolean validateBatchIngestion() {
    if (!deferredWrite) {
      return true;
    }
    if (!super.validateBatchIngestion()) {
      return false;
    }
    return rocksDBSstFileWriter.validateBatchIngestion();
  }

  @Override
  public synchronized void drop() {
    super.deleteSSTFiles(fullPathForTempSSTFileDir);
    super.drop();
  }
}
