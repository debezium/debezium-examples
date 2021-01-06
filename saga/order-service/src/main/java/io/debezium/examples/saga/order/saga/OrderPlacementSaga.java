/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.order.saga;

import static io.debezium.examples.saga.order.saga.OrderPlacementSaga.CREDIT_APPROVAL;
import static io.debezium.examples.saga.order.saga.OrderPlacementSaga.PAYMENT;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.examples.saga.framework.Saga;
import io.debezium.examples.saga.framework.SagaBase;
import io.debezium.examples.saga.framework.SagaStatus;
import io.debezium.examples.saga.framework.SagaStepMessage;
import io.debezium.examples.saga.order.event.CreditApprovalEvent;
import io.debezium.examples.saga.order.event.PaymentEvent;
import io.debezium.examples.saga.order.model.PurchaseOrder;
import io.debezium.examples.saga.order.model.PurchaseOrderStatus;

@Saga(type="order-placement", stepIds = {CREDIT_APPROVAL, PAYMENT})
public class OrderPlacementSaga extends SagaBase {

    protected static final String PAYMENT = "payment";
    protected static final String CREDIT_APPROVAL = "credit-approval";

    private static ObjectMapper objectMapper = new ObjectMapper();

    public OrderPlacementSaga(UUID id, String payload) {
        super(id, payload);
    }

    public static OrderPlacementSaga forPurchaseOrder(PurchaseOrder purchaseOrder) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("order-id", purchaseOrder.id);
            payload.put("customer-id", purchaseOrder.customerId);
            payload.put("payment-due", purchaseOrder.paymentDue);
            payload.put("credit-card-no", purchaseOrder.creditCardNo);
            payload.put("status", "REQUESTED");

            return new OrderPlacementSaga(UUID.randomUUID(), objectMapper.writeValueAsString(payload));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SagaStepMessage getStepMessage(String id) {
        if (id.equals(PAYMENT)) {
            return new SagaStepMessage(PAYMENT, getPayload());
        }
        else {
            return new SagaStepMessage(CREDIT_APPROVAL, getPayload());
        }
    }

    @Override
    public SagaStepMessage getCompensatingStepMessage(String id) {
        if (id.equals(PAYMENT)) {
            return new SagaStepMessage(PAYMENT, "{ \"status\" : \"CANCELLED\", \"order-id\" : " + getOrderId() + "}");
        }
        else {
            return new SagaStepMessage(CREDIT_APPROVAL, "{ \"status\" : \"CANCELLED\", \"order-id\" : " + getOrderId() + "}");
        }
    }

    public void onPaymentEvent(UUID messageId, PaymentEvent event) {
        if (alreadyProcessed(messageId)) {
            return;
        }

        updateStepStatus(PAYMENT, event.status.toStepStatus());
        updateOrderStatus();

        processed(messageId);
    }

    public void onCreditApprovalEvent(UUID messageId, CreditApprovalEvent event) {
        if (alreadyProcessed(messageId)) {
            return;
        }

        updateStepStatus(CREDIT_APPROVAL, event.status.toStepStatus());
        updateOrderStatus();

        processed(messageId);
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

    private long getOrderId() {
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {};
        try {
            HashMap<String, Object> state = objectMapper.readValue(getPayload(), typeRef);
            return (int) state.get("order-id");
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
