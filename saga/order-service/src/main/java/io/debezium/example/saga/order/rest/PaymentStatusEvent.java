package io.debezium.example.saga.order.rest;

import java.util.UUID;

public class PaymentStatusEvent {

    public UUID sagaId;
    public PaymentStatus status;
}
