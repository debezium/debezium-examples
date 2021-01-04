/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.example.saga.order.saga;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.example.saga.framework.SagaBase;
import io.debezium.example.saga.framework.SagaStatus;
import io.debezium.example.saga.framework.SagaStepMessage;
import io.debezium.example.saga.order.model.PurchaseOrder;
import io.debezium.example.saga.order.model.PurchaseOrderStatus;
import io.debezium.example.saga.order.rest.CreditApprovalEvent;
import io.debezium.example.saga.order.rest.PaymentEvent;

public class OrderPlacementSaga extends SagaBase {

    private static final String PAYMENT = "payment";
    private static final String CREDIT_APPROVAL = "credit-approval";

    private static ObjectMapper objectMapper = new ObjectMapper();

    private UUID id;
    private String payload;

    public OrderPlacementSaga(UUID id, String payload) {
        this.id = id;
        this.payload = payload;
    }

    public static OrderPlacementSaga forPurchaseOrder(PurchaseOrder purchaseOrder) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("order-id", purchaseOrder.id);
            payload.put("customer-id", purchaseOrder.customerId);
            payload.put("payment-due", purchaseOrder.paymentDue);
            payload.put("credit-card-no", purchaseOrder.creditCardNo);

            return new OrderPlacementSaga(UUID.randomUUID(), objectMapper.writeValueAsString(payload));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getType() {
        return "order-placement";
    }

    @Override
    public String getPayload() {
        return payload;
    }

    @Override
    public Set<String> stepIds() {
        return new HashSet<>(Arrays.asList(CREDIT_APPROVAL, PAYMENT));
    }

    @Override
    public SagaStepMessage getStepMessage(String id) {
        if (id.equals(PAYMENT)) {
            return new SagaStepMessage(PAYMENT, payload);
        }
        else {
            return new SagaStepMessage(CREDIT_APPROVAL, payload);
        }
    }

    @Override
    public SagaStepMessage getCompensatingStepMessage(String id) {
        if (id.equals(PAYMENT)) {
            return new SagaStepMessage(PAYMENT, "{ \"aborted\" : true }");
        }
        else {
            return new SagaStepMessage(CREDIT_APPROVAL, "{ \"aborted\" : true }");
        }
    }

    public void onPaymentEvent(PaymentEvent event) {
        updateStepStatus(PAYMENT, event.status.toStepStatus());
        updateOrderStatus();
    }

    public void onCreditApprovalEvent(CreditApprovalEvent event) {
        updateStepStatus(CREDIT_APPROVAL, event.status.toStepStatus());
        updateOrderStatus();
    }

    private void updateOrderStatus() {
        if (getStatus() == SagaStatus.COMPLETED) {
            PurchaseOrder order = PurchaseOrder.findById(getOrderId());
            order.status = PurchaseOrderStatus.PROCESSING;
        }
        else if (getStatus() == SagaStatus.ABORTED) {
            PurchaseOrder order = PurchaseOrder.findById(getOrderId());
            order.status = PurchaseOrderStatus.CANCELLED;
        }
    }

    public long getOrderId() {
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {};
        try {
            HashMap<String, Object> state = objectMapper.readValue(payload, typeRef);
            return (int) state.get("order-id");
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
