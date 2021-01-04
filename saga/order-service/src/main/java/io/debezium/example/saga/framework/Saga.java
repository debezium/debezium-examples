package io.debezium.example.saga.framework;

import java.util.Set;
import java.util.UUID;

public interface Saga {

    UUID getId();
    String getType();
    String getPayload();
    Set<String> stepIds();
    SagaStepEvent getStepEvent(String id);
    SagaStepEvent getCompensatingStep(String id);
}
