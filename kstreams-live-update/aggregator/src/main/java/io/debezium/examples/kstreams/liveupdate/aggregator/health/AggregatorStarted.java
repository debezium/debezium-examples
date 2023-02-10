/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator.health;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.streams.KafkaStreams;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import io.debezium.examples.kstreams.liveupdate.aggregator.StreamsPipelineManager;

@Liveness
@ApplicationScoped
public class AggregatorStarted implements HealthCheck {

    @Inject
    StreamsPipelineManager spm;

    @Inject
    KafkaStreams streams;

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("aggregator")
                .status(streams.state().equals(KafkaStreams.State.RUNNING))
                .build();
    }
}
