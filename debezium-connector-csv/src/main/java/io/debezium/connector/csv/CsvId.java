/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.spi.schema.DataCollectionId;

import java.util.List;

/**
 * Identifies the single "table" tracked by a CSV connector instance.
 *
 * <p>The identifier is the base name of the database file (filename without extension),
 * e.g. {@code employees} for {@code /data/employees.csv}.
 * This value is also used as the table segment in the Kafka topic name:
 * {@code <topic.prefix>.<tableName>}.
 */
public class CsvId implements DataCollectionId {

    private final String tableName;

    public CsvId(String tableName) {
        this.tableName = tableName;
    }

    /** Used by the framework for topic naming and offset storage. */
    @Override
    public String identifier() {
        return tableName;
    }

    @Override
    public List<String> parts() {
        return List.of(tableName);
    }

    @Override
    public List<String> databaseParts() {
        return List.of();
    }

    @Override
    public List<String> schemaParts() {
        return List.of(tableName);
    }

    @Override
    public String toString() {
        return tableName;
    }
}