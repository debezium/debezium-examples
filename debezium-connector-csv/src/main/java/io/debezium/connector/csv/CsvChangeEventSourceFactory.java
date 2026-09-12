/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.pipeline.EventDispatcher;
import io.debezium.pipeline.notification.NotificationService;
import io.debezium.pipeline.source.spi.ChangeEventSourceFactory;
import io.debezium.pipeline.source.spi.SnapshotChangeEventSource;
import io.debezium.pipeline.source.spi.SnapshotProgressListener;
import io.debezium.pipeline.source.spi.StreamingChangeEventSource;

/**
 * Creates the snapshot and streaming change event sources for the CSV connector.
 *
 * <p>Called by {@link io.debezium.pipeline.ChangeEventSourceCoordinator} once per connector start.
 * Both sources share the same {@link CsvConnectorConfig}, {@link CsvId}, {@link CsvSchema}
 * and {@link EventDispatcher} instances that are wired together in {@link CsvConnectorTask}.
 */
class CsvChangeEventSourceFactory implements ChangeEventSourceFactory<CsvPartition, CsvOffsetContext> {

    private final CsvConnectorConfig config;
    private final CsvId csvId;
    private final CsvSchema schema;
    private final EventDispatcher<CsvPartition, CsvId> dispatcher;

    CsvChangeEventSourceFactory(CsvConnectorConfig config,
                                 CsvId csvId,
                                 CsvSchema schema,
                                 EventDispatcher<CsvPartition, CsvId> dispatcher) {
        this.config = config;
        this.csvId = csvId;
        this.schema = schema;
        this.dispatcher = dispatcher;
    }

    @Override
    public SnapshotChangeEventSource<CsvPartition, CsvOffsetContext> getSnapshotChangeEventSource(
            SnapshotProgressListener<CsvPartition> progressListener,
            NotificationService<CsvPartition, CsvOffsetContext> notificationService) {
        return new CsvSnapshotChangeEventSource(config, csvId, schema, dispatcher, progressListener);
    }

    @Override
    public StreamingChangeEventSource<CsvPartition, CsvOffsetContext> getStreamingChangeEventSource() {
        return new CsvStreamingChangeEventSource(config, csvId, schema, dispatcher);
    }
}
