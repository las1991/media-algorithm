package com.sengled.media.worker;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.codahale.metrics.MetricRegistry;

/**
 * kinesis stream record 处理器
 * @author liwei
 * @Date   2017年3月2日 下午3:28:28 
 * @Desc
 */
public class RecordProcessor implements IRecordProcessor {
    private static final Log LOGGER = LogFactory.getLog(RecordProcessor.class);
    private String kinesisShardId;
    private ProcessorManager processorManager;
    
    // Checkpointing interval
    private static final long CHECKPOINT_INTERVAL_MILLIS = 60000L; // 1 minute
    private long nextCheckpointTimeInMillis;
    private AtomicLong recordCount;
    
    public RecordProcessor(ProcessorManager processorManager,AtomicLong recordCount){
        this.processorManager = processorManager;
        this.recordCount = recordCount;
    }
    @Override
    public void initialize(String shardId) {
        LOGGER.info("Initializing record processor for shard: " + shardId);
        this.kinesisShardId = shardId;
        this.nextCheckpointTimeInMillis = CHECKPOINT_INTERVAL_MILLIS;
    }
 
    @Override
    public void processRecords(List<Record> records, IRecordProcessorCheckpointer checkpointer) {
        LOGGER.info("received records...");
        recordCount.addAndGet(records.size());
        for (Record record : records) {
            processRecord(record);
        }
        // Checkpoint once every checkpoint interval
        if (System.currentTimeMillis() > nextCheckpointTimeInMillis) {
            checkpoint(checkpointer);
            nextCheckpointTimeInMillis = System.currentTimeMillis() + CHECKPOINT_INTERVAL_MILLIS;
        }
    }


    private void processRecord(Record record) {
        try {
            ByteBuffer buffer = record.getData();
            if(buffer.remaining()<=0){
                LOGGER.error("record data size is null.PartitionKey:"+record.getPartitionKey());
                return;
            }
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            processorManager.submit(record.getPartitionKey(), data);
        } catch (Exception e) {
            LOGGER.error("Record Submit Exception:"+e.getMessage(),e);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown(IRecordProcessorCheckpointer checkpointer, ShutdownReason reason) {
        LOGGER.info("Shutting down record processor for shard: " + kinesisShardId);
        if (reason == ShutdownReason.TERMINATE) {
            checkpoint(checkpointer);
        }
    }
    private void checkpoint(IRecordProcessorCheckpointer checkpointer) {
        LOGGER.info("Checkpointing shard " + kinesisShardId);
        try {
            checkpointer.checkpoint();
        } catch (ShutdownException se) {
            // Ignore checkpoint if the processor instance has been shutdown (fail over).
            LOGGER.info("Caught shutdown exception, skipping checkpoint.", se);
        } catch (ThrottlingException e) {
            // Skip checkpoint when throttled. In practice, consider a backoff and retry policy.
            LOGGER.error("Caught throttling exception, skipping checkpoint.", e);
        } catch (InvalidStateException e) {
            // This indicates an issue with the DynamoDB table (check for table, provisioned IOPS).
            LOGGER.error("Cannot save checkpoint to the DynamoDB table used by the Amazon Kinesis Client Library.", e);
        }
    }
}
