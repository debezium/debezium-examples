package io.debezium.example.saga.order.rest;

import java.util.UUID;

public class CreditApprovalStatusEvent {

    public UUID sagaId;
    public CreditApprovalStatus status;
}
