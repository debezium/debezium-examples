/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator.health;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import io.debezium.examples.kstreams.liveupdate.aggregator.StreamsPipelineManager;

@Health
@ApplicationScoped
public class AggregatorStarted implements HealthCheck {

    @Inject
    private StreamsPipelineManager spm;

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("aggregator")
                .withData("KStreams pipeline started", spm.isStarted())
                .state(spm.isStarted())
                .build();
    }
}
