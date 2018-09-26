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
