/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.eventsource;

import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="temperature_measurements")
public class TemperatureMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    public long id;

    @ManyToOne
    public Station station;

    public double value;

    @Column(name = "ts")
    public ZonedDateTime timestamp;

    TemperatureMeasurement() {
    }

    public TemperatureMeasurement(Station station, double value, ZonedDateTime timestamp) {
        this.station = station;
        this.value = value;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "TemperatureMeasurement [id=" + id + ", station=" + station + ", value=" + value + ", timestamp="
                + timestamp + "]";
    }
}
