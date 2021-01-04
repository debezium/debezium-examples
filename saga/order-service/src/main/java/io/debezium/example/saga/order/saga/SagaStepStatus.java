package io.debezium.example.saga.order.saga;

public enum SagaStepStatus {
    STARTED, FAILED, SUCCEEDED, ABORTING, ABORTED; //, COMPENSATING, COMPENSATED;
}
