package io.debezium.example.saga.order.saga;

public enum SagaStatus {
    STARTED, ABORTING, ABORTED, COMPLETED;
}
