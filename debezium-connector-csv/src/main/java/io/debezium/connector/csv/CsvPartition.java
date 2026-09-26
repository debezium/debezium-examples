/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.pipeline.spi.Partition;

import java.util.Map;

/**
 * Identifies the offset partition for a CSV connector instance.
 *
 * <p>Each connector instance tracks a single file. The partition key is the logical server name ({@code topic.prefix}),
 * so two connector instances targeting different files under different prefixes keep independent offsets.
 */
public class CsvPartition implements Partition {

    private static final String SERVER_KEY = "server";

    private final String serverName;

    public CsvPartition(String serverName) {
        this.serverName = serverName;
    }

    /**
     * Returns the Kafka Connect source partition map.
     * This map is used as the key when storing/loading offsets via the
     * Connect framework's offset storage.
     */
    @Override
    public Map<String, String> getSourcePartition() {
        return Map.of(SERVER_KEY, serverName);
    }
}
