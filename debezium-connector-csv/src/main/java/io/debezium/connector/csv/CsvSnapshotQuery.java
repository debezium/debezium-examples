/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.snapshot.spi.SnapshotQuery;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * No-op {@link SnapshotQuery} for the CSV connector.
 * CSV data is read directly from the file; SQL queries are not applicable.
 */
class CsvSnapshotQuery implements SnapshotQuery {

    @Override
    public String name() {
        return "csv";
    }

    @Override
    public Optional<String> snapshotQuery(String s, List<String> list) {
        // CSV data is read directly from the file, not via a query
        return Optional.empty();
    }

    @Override
    public void configure(Map<String, ?> properties) {
        // no configuration needed
    }
}
