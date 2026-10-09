/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

/**
 * Definition of a single column parsed from a CSV schema header.
 *
 * @param name the column name
 * @param type the column type
 */
record ColumnDef(String name, ColumnType type) {

    /**
     * Parses a column spec of the form {@code name} or {@code name:TYPE}.
     * Unknown type tokens are treated as {@link ColumnType#STRING}.
     */
    static ColumnDef parse(String spec) {
        int colon = spec.indexOf(':');
        if (colon < 0) {
            return new ColumnDef(spec.trim(), ColumnType.STRING);
        }
        String name = spec.substring(0, colon).trim();
        String typePart = spec.substring(colon + 1).trim().toUpperCase();
        ColumnType type;
        try {
            type = ColumnType.valueOf(typePart);
        }
        catch (IllegalArgumentException e) {
            type = ColumnType.STRING;
        }
        return new ColumnDef(name, type);
    }
}
