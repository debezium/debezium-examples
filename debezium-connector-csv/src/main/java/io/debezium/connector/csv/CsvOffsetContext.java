/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.pipeline.CommonOffsetContext;
import io.debezium.pipeline.txmetadata.TransactionContext;
import io.debezium.spi.schema.DataCollectionId;
import org.apache.kafka.connect.data.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Tracks the current CSV read position.
 *
 * <p>The offset is stored as {@code {"line": N}}, where {@code N} is the next line to read.
 * This is used for restart recovery and snapshot-to-streaming handoff.
 */
public class CsvOffsetContext extends CommonOffsetContext<CsvSourceInfo> {

    static final String LINE_OFFSET_KEY = "line";

    /** Zero-based index of the next line to read. */
    private long lineNumber;

    public CsvOffsetContext(CsvSourceInfo sourceInfo) {
        super(sourceInfo);
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(long lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Serialises the current position into the map that is persisted by
     * the Kafka Connect offset storage.
     */
    @Override
    public Map<String, ?> getOffset() {
        return Map.of(LINE_OFFSET_KEY, lineNumber);
    }

    @Override
    public Schema getSourceInfoSchema() {
        return sourceInfo.schema();
    }

    /**
     * Called by the framework before each event is enqueued.
     * Updates the source info so that the line number is reflected in the
     * {@code source} block of the outgoing message.
     */
    @Override
    public void event(DataCollectionId dataCollectionId, Instant instant) {
        sourceInfo.setLineNumber(lineNumber);
    }

    /** CSV has no transactions; return an empty context. */
    @Override
    public TransactionContext getTransactionContext() {
        return new TransactionContext();
    }

    /** Returns the source info as a Struct for embedding in envelope records. */
    org.apache.kafka.connect.data.Struct getSourceInfoStruct() {
        return sourceInfo.struct();
    }
}
