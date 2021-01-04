package io.debezium.example.saga.framework;

import io.debezium.example.saga.order.saga.SagaStepStatus;

public class SagaStepState {

    public String type;
    public SagaStepStatus status;

    public SagaStepState(String type, SagaStepStatus status) {
        this.type = type;
        this.status = status;
    }
}
