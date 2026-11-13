/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.config.Configuration;
import io.debezium.connector.common.BaseSourceConnector;
import io.debezium.spi.schema.DataCollectionId;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigValue;
import org.apache.kafka.connect.connector.Task;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Top-level Kafka Connect connector for the CSV connector.
 *
 * <p>Always runs exactly one {@link CsvConnectorTask} — one connector instance monitors
 * one file. {@link #validateAllFields} checks that the configured file path exists at
 * deployment time, providing an early failure instead of a cryptic runtime error.
 */
public class CsvSourceConnector extends BaseSourceConnector {

    private Map<String, String> properties;

    @Override
    public void start(Map<String, String> properties) {
        this.properties = Map.copyOf(properties);
    }

    @Override
    public Class<? extends Task> taskClass() {
        return CsvConnectorTask.class;
    }

    /**
     * Returns a single-element list — this connector always uses exactly one task.
     *
     * @param maxTasks ignored; single-file connectors do not parallelize across tasks
     */
    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        return List.of(Map.copyOf(properties));
    }

    @Override
    public void stop() {
        // Kafka Connect manages task lifecycle; nothing to do here.
    }

    @Override
    public ConfigDef config() {
        return CsvConnectorConfig.configDef();
    }

    @Override
    public String version() {
        return "1.0";
    }

    /** Validates that the configured CSV file path exists on the local filesystem. */
    @Override
    protected Map<String, ConfigValue> validateAllFields(Configuration configuration) {
        Map<String, ConfigValue> results = new java.util.LinkedHashMap<>();

        String rawPath = configuration.getString(CsvConnectorConfig.FILE_PATH);
        if (rawPath != null && !rawPath.isBlank() && !Files.exists(Paths.get(rawPath))) {
            ConfigValue cv = new ConfigValue(CsvConnectorConfig.FILE_PATH.name());
            cv.addErrorMessage("File does not exist: " + rawPath);
            results.put(CsvConnectorConfig.FILE_PATH.name(), cv);
        }

        return results;
    }

    @Override
    public <T extends DataCollectionId> List<T> getMatchingCollections(Configuration configuration) {
        return List.of();
    }
}
