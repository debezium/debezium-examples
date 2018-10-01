/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator.model;

public class Station {

    public long id;

    public String name;

    public Station(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Station [id=" + id + ", name=" + name + "]";
    }
}
