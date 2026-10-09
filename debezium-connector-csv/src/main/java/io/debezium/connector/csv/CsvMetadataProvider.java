/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.pipeline.source.spi.EventMetadataProvider;
import io.debezium.pipeline.spi.OffsetContext;
import io.debezium.spi.schema.DataCollectionId;
import org.apache.kafka.connect.data.Struct;

import java.time.Instant;
import java.util.Map;

/**
 * Provides event-level metadata used by the framework for logging and monitoring.
 */
class CsvMetadataProvider implements EventMetadataProvider {

    @Override
    public Instant getEventTimestamp(DataCollectionId dataCollectionId, OffsetContext offsetContext, Object key, Struct value) {
        return Instant.now();
    }

    @Override
    public Map<String, String> getEventSourcePosition(DataCollectionId dataCollectionId, OffsetContext offsetContext, Object key, Struct value) {
        if (offsetContext instanceof CsvOffsetContext ctx) {
            return Map.of(CsvOffsetContext.LINE_OFFSET_KEY, String.valueOf(ctx.getLineNumber()));
        }
        return Map.of();
    }

    /** CSV has no transactions; return empty string. */
    @Override
    public String getTransactionId(DataCollectionId dataCollectionId, OffsetContext offsetContext, Object key, Struct value) {
        return "";
    }
}
