/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.example.saga.framework;

import java.util.Set;
import java.util.UUID;

public interface Saga {

    UUID getId();
    SagaStatus getStatus();
    String getType();
    String getPayload();
    Set<String> stepIds();
    SagaStepMessage getStepMessage(String id);
    SagaStepMessage getCompensatingStepMessage(String id);
}
