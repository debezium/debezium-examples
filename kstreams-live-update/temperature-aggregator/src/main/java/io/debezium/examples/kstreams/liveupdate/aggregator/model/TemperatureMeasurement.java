/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator.model;

import java.time.ZonedDateTime;

public class TemperatureMeasurement {

    public long id;

    public long stationId;

    public String stationName;

    public double value;

    public ZonedDateTime timestamp;

    public TemperatureMeasurement(long id, long stationId, double value, ZonedDateTime timestamp) {
        this.id = id;
        this.stationId = stationId;
        this.value = value;
        this.timestamp = timestamp;
    }

    public TemperatureMeasurement(long id, long stationId, String stationName, double value, ZonedDateTime timestamp) {
        this.id = id;
        this.stationId = stationId;
        this.stationName = stationName;
        this.value = value;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "TemperatureMeasurement [id=" + id + ", stationId=" + stationId + ", stationName=" + stationName
                + ", value=" + value + ", timestamp=" + timestamp + "]";
    }
}
