/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.pipeline.spi.OffsetContext;

import java.util.Map;

/**
 * Restores a {@link CsvOffsetContext} from Kafka Connect's persisted offset map.
 *
 * <p>The stored map format is {@code {"line": N}}. On first start the map is
 * {@code null}/empty, producing a zero-offset context that triggers a fresh snapshot.
 * On subsequent starts streaming resumes from the persisted line number.
 */
public class CsvOffsetLoader implements OffsetContext.Loader<CsvOffsetContext> {

    private final CsvConnectorConfig csvConnectorConfig;

    public CsvOffsetLoader(CsvConnectorConfig csvConnectorConfig) {
        this.csvConnectorConfig = csvConnectorConfig;
    }

    @Override
    public CsvOffsetContext load(Map<String, ?> map) {
        CsvOffsetContext ctx = new CsvOffsetContext(new CsvSourceInfo(csvConnectorConfig));
        if (map != null) {
            Object lineValue = map.get(CsvOffsetContext.LINE_OFFSET_KEY);
            if (lineValue != null) {
                ctx.setLineNumber(Long.parseLong(lineValue.toString()));
            }
        }
        // lineNumber stays 0 when map is null/empty → triggers snapshot on first start
        return ctx;
    }
}
