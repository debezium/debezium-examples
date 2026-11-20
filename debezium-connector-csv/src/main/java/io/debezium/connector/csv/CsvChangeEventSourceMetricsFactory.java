/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.connector.base.ChangeEventQueueMetrics;
import io.debezium.connector.common.CdcSourceTaskContext;
import io.debezium.pipeline.metrics.DefaultChangeEventSourceMetricsFactory;
import io.debezium.pipeline.metrics.SnapshotChangeEventSourceMetrics;
import io.debezium.pipeline.metrics.StreamingChangeEventSourceMetrics;
import io.debezium.pipeline.metrics.spi.ChangeEventSourceMetricsFactory;
import io.debezium.pipeline.source.spi.EventMetadataProvider;

/**
 * Provides standard Debezium JMX metrics for snapshot and streaming phases.
 *
 * <p>Delegates to {@link DefaultChangeEventSourceMetricsFactory}, which exposes
 * generic metrics (records captured, events per second, etc.) without requiring
 * any connector-specific instrumentation.
 */
class CsvChangeEventSourceMetricsFactory implements ChangeEventSourceMetricsFactory<CsvPartition> {

    private final DefaultChangeEventSourceMetricsFactory<CsvPartition> delegate =
            new DefaultChangeEventSourceMetricsFactory<>();

    @Override
    public <T extends CdcSourceTaskContext> SnapshotChangeEventSourceMetrics<CsvPartition> getSnapshotMetrics(
            T context, ChangeEventQueueMetrics queueMetrics, EventMetadataProvider metadataProvider) {
        return delegate.getSnapshotMetrics(context, queueMetrics, metadataProvider);
    }

    @Override
    public <T extends CdcSourceTaskContext> StreamingChangeEventSourceMetrics<CsvPartition> getStreamingMetrics(
            T context, ChangeEventQueueMetrics queueMetrics, EventMetadataProvider metadataProvider) {
        return delegate.getStreamingMetrics(context, queueMetrics, metadataProvider);
    }
}
