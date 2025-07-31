package org.acme;

import io.debezium.runtime.events.ConnectorStartedEvent;
import io.quarkus.debezium.notification.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DebeziumMonitor {

    private final SnapshotService snapshotService;

    private static final Logger logger = LoggerFactory.getLogger(DebeziumMonitor.class);

    @Inject
    public DebeziumMonitor(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    public void snapshot(@Observes SnapshotEvent event) {
        switch (event) {
            case SnapshotAborted aborted -> logger.info("the snapshot is aborted: {}", aborted.getAdditionalData());
            case SnapshotCompleted completed -> logger.info("the snapshot is completed: {}", completed.getAdditionalData());
            case SnapshotInProgress inProgress -> logger.info("the snapshot is in progress: {}", inProgress.getAdditionalData());
            case SnapshotPaused paused -> logger.info("the snapshot is in paused: {}", paused.getAdditionalData());
            case SnapshotResumed resumed -> logger.info("the snapshot is in resumed: {}", resumed.getAdditionalData());
            case SnapshotSkipped skipped -> logger.info("the snapshot is in skipped: {}", skipped.getAdditionalData());
            case SnapshotStarted started -> logger.info("the snapshot is in started: {}", started.getAdditionalData());
            case SnapshotTableScanCompleted tablescan -> logger.info("the snapshot is in tablescan: {}", tablescan.getAdditionalData());
        }

        snapshotService.send(event);
    }

    public void notification(@Observes DebeziumNotification event) {
        logger.info("received a notification {}", event.aggregateType());
    }

    public void connectorStarted(@Observes ConnectorStartedEvent event) {
        logger.info("connector is started for connector {}", event.getEngine().connector());
    }
}
