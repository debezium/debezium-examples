/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.config.CommonConnectorConfig;
import io.debezium.connector.common.BaseSourceInfo;

import java.time.Instant;

/**
 * Carries the {@code source} metadata block that is included in every change event.
 *
 * <p>For CSV the relevant source information is the name of the file being
 * monitored and the line number of the record that triggered the event.
 * Both values are populated by {@link CsvOffsetContext#event} before each event
 * is emitted, and exposed via {@link CsvSourceInfoStructMaker}.
 */
public class CsvSourceInfo extends BaseSourceInfo {

    private final CommonConnectorConfig config;
    private String fileName = "";
    private long lineNumber;

    public CsvSourceInfo(CommonConnectorConfig config) {
        super(config);
        this.config = config;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setLineNumber(long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    /** Returns the current wall-clock time as the event timestamp. */
    @Override
    protected Instant timestamp() {
        return Instant.now();
    }

    /** Returns the logical server name ({@code topic.prefix}) as the database name. */
    @Override
    protected String database() {
        return config.getLogicalName();
    }
}
