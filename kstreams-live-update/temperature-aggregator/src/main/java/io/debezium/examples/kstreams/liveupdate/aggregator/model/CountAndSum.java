/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator.model;

public class CountAndSum {

    public long count;
    public long sum;

    public CountAndSum() {
    }

    public CountAndSum(long count, long sum) {
        this.count = count;
        this.sum = sum;
    }
}
