/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;
import io.debezium.schema.DataCollectionFilters;

/**
 * Determines which data collections to include when producing change events.
 *
 * The CSV connector tracks a single file, so all collections are always included.
 */
class CsvDataCollectionFilter implements DataCollectionFilters.DataCollectionFilter<CsvId> {

    @Override
    public boolean isIncluded(CsvId csvId) {
        return true;
    }
}