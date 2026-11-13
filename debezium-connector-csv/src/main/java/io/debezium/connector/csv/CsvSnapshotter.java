/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.spi.snapshot.Snapshotter;

import java.util.Map;

/**
 * Initial-mode snapshot strategy for the CSV connector.
 *
 * <p>Triggers a snapshot when no offset has been stored yet, then streams from the
 * handover line. The fine-grained snapshot/skip decision is made in
 * {@link CsvSnapshotChangeEventSource#getSnapshottingTask} via the line-number offset.
 */
class CsvSnapshotter implements Snapshotter {

    @Override
    public String name() {
        return "initial";
    }

    /**
     * @param offsetExists      {@code false} on first start (no stored offset)
     * @param snapshotInProgress {@code true} if a previous snapshot was interrupted
     */
    @Override
    public boolean shouldSnapshotData(boolean offsetExists, boolean snapshotInProgress) {
        return !offsetExists || snapshotInProgress;
    }

    /** Schema is derived from the CSV header line, not from a separate schema snapshot. */
    @Override
    public boolean shouldSnapshotSchema(boolean offsetExists, boolean snapshotInProgress) {
        return false;
    }

    /** Always stream after snapshot. */
    @Override
    public boolean shouldStream() {
        return true;
    }

    @Override
    public boolean shouldSnapshotOnSchemaError() {
        return false;
    }

    @Override
    public boolean shouldSnapshotOnDataError() {
        return false;
    }

    @Override
    public void configure(Map<String, ?> properties) {
        // no configuration needed
    }
}
