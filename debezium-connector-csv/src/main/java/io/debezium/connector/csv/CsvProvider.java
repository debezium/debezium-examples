/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.pipeline.spi.Partition;

import java.util.Collections;
import java.util.Set;

/**
 * Supplies the set of partitions that this connector instance manages.
 *
 * <p>A CSV connector tracks exactly one file, so there is always a single
 * partition keyed on the logical server name ({@code topic.prefix}).
 */
public class CsvProvider implements Partition.Provider<CsvPartition> {

    private final String serverName;

    public CsvProvider(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public Set<CsvPartition> getPartitions() {
        return Collections.singleton(new CsvPartition(serverName));
    }
}
