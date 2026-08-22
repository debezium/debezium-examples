/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

/**
 * Supported column types in a CSV schema header.
 *
 * <p>Example header line:
 * {@code EventType;Id:INT;FirstName:STRING;Salary:INT;Active:BOOLEAN;Birthday:DATE}
 *
 * <p>If no type is specified the column defaults to {@link #STRING}.
 */
enum ColumnType {
    STRING,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    BOOLEAN,
    DATE
}
