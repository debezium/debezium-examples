/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.config.CommonConnectorConfig;
import io.debezium.config.Configuration;
import io.debezium.config.EnumeratedValue;
import io.debezium.config.Field;
import io.debezium.connector.SourceInfoStructMaker;
import org.apache.kafka.common.config.ConfigDef;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Connector configuration for the CSV connector.
 *
 * <p>Extends {@link CommonConnectorConfig} with one connector-specific field:
 * {@link #FILE_PATH}. All other settings (topic prefix, poll interval, batch size)
 * are inherited. The file's base name (without extension) becomes the Kafka topic suffix.
 *
 * <p>Minimum required properties: {@code connector.class}, {@code topic.prefix},
 * {@code csv.file.path}.
 */
public class CsvConnectorConfig extends CommonConnectorConfig {

    /**
     * Snapshot strategy enum. Only {@code INITIAL} is supported: snapshot on first start,
     * stream from the stored line-number offset on subsequent starts.
     */
    public enum SnapshotMode implements EnumeratedValue {
        INITIAL("initial");

        private final String value;

        SnapshotMode(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    /**
     * Absolute path to the CSV database file (e.g. {@code /data/employees.csv}).
     * The filename without extension becomes the table name and Kafka topic suffix.
     */
    public static final Field FILE_PATH = Field.create("csv.file.path")
            .withDisplayName("CSV file path")
            .withType(ConfigDef.Type.STRING)
            .withWidth(ConfigDef.Width.LONG)
            .withImportance(ConfigDef.Importance.HIGH)
            .withDescription("Absolute path to the CSV database file, e.g. /data/employees.csv")
            .required();

    static final Field.Set ALL_FIELDS = Field.setOf(FILE_PATH);

    public CsvConnectorConfig(Configuration config, int defaultSnapshotFetchSize) {
        super(config, defaultSnapshotFetchSize);
    }

    /** Returns the Kafka Connect {@link ConfigDef} used to validate configuration at deployment. */
    public static ConfigDef configDef() {
        ConfigDef config = new ConfigDef();
        Field.group(config, "CSV", FILE_PATH);
        return config;
    }

    public Path getFilePath() {
        return Paths.get(getConfig().getString(FILE_PATH));
    }

    @Override
    public String getContextName() {
        return "CSV";
    }

    @Override
    public String getConnectorName() {
        return "csv";
    }

    @Override
    public EnumeratedValue getSnapshotMode() {
        return SnapshotMode.INITIAL;
    }

    @Override
    public Optional<? extends EnumeratedValue> getSnapshotLockingMode() {
        return Optional.empty();
    }

    @Override
    protected SourceInfoStructMaker<?> getSourceInfoStructMaker(Version version) {
        // init() must be called here — CommonConnectorConfig never calls it, but AbstractSourceInfo.schema() needs it.
        CsvSourceInfoStructMaker maker = new CsvSourceInfoStructMaker();
        maker.init(getConnectorName(), "1.0", this);
        return maker;
    }
}
