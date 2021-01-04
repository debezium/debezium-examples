package io.debezium.example.saga.framework;

public class SagaStepEvent {

    public String type;
    public String payload;

    public SagaStepEvent(String type, String payload) {
        this.type = type;
        this.payload = payload;
    }
}
