/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.config.Configuration;
import io.debezium.config.Field;
import io.debezium.connector.base.ChangeEventQueue;
import io.debezium.connector.common.BaseSourceTask;
import io.debezium.connector.common.CdcSourceTaskContext;
import io.debezium.document.DocumentReader;
import io.debezium.pipeline.ChangeEventSourceCoordinator;
import io.debezium.pipeline.DataChangeEvent;
import io.debezium.pipeline.ErrorHandler;
import io.debezium.pipeline.EventDispatcher;
import io.debezium.pipeline.metrics.spi.ChangeEventSourceMetricsFactory;
import io.debezium.pipeline.notification.NotificationService;
import io.debezium.pipeline.signal.SignalProcessor;
import io.debezium.pipeline.source.spi.ChangeEventSourceFactory;
import io.debezium.pipeline.spi.Offsets;
import io.debezium.schema.DataCollectionFilters;
import io.debezium.schema.SchemaFactory;
import io.debezium.snapshot.SnapshotterService;
import io.debezium.snapshot.lock.NoLockingSupport;
import io.debezium.spi.topic.TopicNamingStrategy;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Kafka Connect {@link org.apache.kafka.connect.source.SourceTask} for the CSV connector.
 *
 * <p>{@link #start} wires all Debezium infrastructure (config, schema, dispatcher, metrics, coordinator)
 * and starts the coordinator, which runs snapshot and streaming phases on background threads.
 * {@link #doPoll} drains the internal
 * {@link ChangeEventQueue} on each Connect poll cycle.
 * {@link #doStop} is a no-op;
 * the coordinator manages its own shutdown.
 */
public class CsvConnectorTask extends BaseSourceTask<CsvPartition, CsvOffsetContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvConnectorTask.class);

    /** Held as a field so that {@link #doPoll()} can drain it. */
    private volatile ChangeEventQueue<DataChangeEvent> changeEventQueue;
    private volatile ErrorHandler errorHandler;

    @Override
    protected ChangeEventSourceCoordinator<CsvPartition, CsvOffsetContext> start(Configuration configuration) {

        // 1. config
        CsvConnectorConfig connectorConfig = new CsvConnectorConfig(configuration, 0);

        // 2. derive table name from filename (e.g. employees.csv → "employees")
        String fileName = connectorConfig.getFilePath().getFileName().toString();
        String tableName = fileName.contains(".")
                ? fileName.substring(0, fileName.lastIndexOf('.'))
                : fileName;
        CsvId csvId = new CsvId(tableName);

        // 3. restore stored offsets; on first start getTheOnlyOffset() returns null — seed a
        //    zero-offset context so snapshot logic sees a valid (not null) offsetContext
        String serverName = connectorConfig.getLogicalName();
        Offsets<CsvPartition, CsvOffsetContext> previousOffsets = getPreviousOffsets(
                new CsvProvider(serverName),
                new CsvOffsetLoader(connectorConfig));

        if (previousOffsets.getTheOnlyOffset() == null) {
            CsvPartition partition = new CsvPartition(serverName);
            CsvOffsetContext offsetContext = new CsvOffsetContext(new CsvSourceInfo(connectorConfig));
            previousOffsets = Offsets.of(partition, offsetContext);
        }

        // 4. task context (needed before queue so it can supply a logging-context)
        CdcSourceTaskContext taskContext = new CdcSourceTaskContext(
                connectorConfig, Map.of(), () -> List.of(csvId));

        // 5. change-event queue buffers records between producer threads and poll()
        changeEventQueue = new ChangeEventQueue.Builder<DataChangeEvent>()
                .pollInterval(connectorConfig.getPollInterval())
                .maxBatchSize(connectorConfig.getMaxBatchSize())
                .maxQueueSize(connectorConfig.getMaxQueueSize())
                .loggingContextSupplier(() -> taskContext.configureLoggingContext("csv"))
                .buffering()
                .build();

        // 6. error handler routes unrecoverable errors back through the queue
        errorHandler = new CsvErrorHandler(connectorConfig, changeEventQueue, errorHandler);

        // 7. JMX metrics
        ChangeEventSourceMetricsFactory<CsvPartition> metricsFactory =
                new CsvChangeEventSourceMetricsFactory();

        // 8. topic naming strategy: <prefix>.<tableName>
        TopicNamingStrategy<CsvId> topicNamingStrategy =
                connectorConfig.getTopicNamingStrategy(CsvConnectorConfig.TOPIC_NAMING_STRATEGY);

        // 9. schema holder — populated lazily from the CSV header line
        CsvSchema schema = new CsvSchema();

        // 10. include-all filter (single-file connector)
        DataCollectionFilters.DataCollectionFilter<CsvId> dataCollectionFilter =
                new CsvDataCollectionFilter();

        // 11. signal processor (Debezium signal channels)
        SignalProcessor<CsvPartition, CsvOffsetContext> signalProcessor = new SignalProcessor<>(
                CsvSourceConnector.class, connectorConfig, Map.of(),
                getAvailableSignalChannels(),
                DocumentReader.defaultReader(),
                previousOffsets);

        // 12. event metadata provider
        CsvMetadataProvider eventMetadataProvider = new CsvMetadataProvider();

        // 13. dispatcher routes records through schema/filter into the change-event queue
        EventDispatcher<CsvPartition, CsvId> eventDispatcher = new EventDispatcher<>(
                connectorConfig,
                topicNamingStrategy,
                schema,
                changeEventQueue,
                dataCollectionFilter,
                DataChangeEvent::new,
                eventMetadataProvider,
                connectorConfig.schemaNameAdjuster());

        // 14. notification service (heartbeats, schema-change notifications)
        NotificationService<CsvPartition, CsvOffsetContext> notificationService =
                new NotificationService<>(
                        getNotificationChannels(),
                        connectorConfig,
                        SchemaFactory.get(),
                        eventDispatcher::enqueueNotification);

        // 15. snapshotter: initial snapshot on first start, stream thereafter
        SnapshotterService snapshotterService = new SnapshotterService(
                new CsvSnapshotter(),
                new CsvSnapshotQuery(),
                new NoLockingSupport());

        // 16. event source factory — must be created AFTER dispatcher (captures a reference to it)
        ChangeEventSourceFactory<CsvPartition, CsvOffsetContext> csvEventSourceFactory =
                new CsvChangeEventSourceFactory(connectorConfig, csvId, schema, eventDispatcher);

        // 17. coordinator orchestrates the snapshot → streaming transition
        ChangeEventSourceCoordinator<CsvPartition, CsvOffsetContext> coordinator =
                new ChangeEventSourceCoordinator<>(
                        previousOffsets,
                        errorHandler,
                        CsvSourceConnector.class,
                        connectorConfig,
                        csvEventSourceFactory,
                        metricsFactory,
                        eventDispatcher,
                        schema,
                        signalProcessor,
                        notificationService,
                        snapshotterService);

        coordinator.start(taskContext, changeEventQueue, eventMetadataProvider);

        return coordinator;
    }

    /**
     * Drains the {@link ChangeEventQueue} and returns the batch to the Connect framework.
     * Blocks up to {@code poll.interval.ms} before returning an empty list.
     */
    @Override
    protected List<SourceRecord> doPoll() throws InterruptedException {
        return changeEventQueue.poll().stream()
                .map(DataChangeEvent::getRecord)
                .toList();
    }

    /** No-op — the coordinator manages its own shutdown. */
    @Override
    protected void doStop() {
    }

    @Override
    protected Iterable<Field> getAllConfigurationFields() {
        return CsvConnectorConfig.ALL_FIELDS;
    }

    @Override
    public String version() {
        return "1.0";
    }
}
