package io.debezium.example.saga.framework;

public interface SagaEventHandler<S extends Saga, T> {
    S onEvent(S saga, T event);
}
