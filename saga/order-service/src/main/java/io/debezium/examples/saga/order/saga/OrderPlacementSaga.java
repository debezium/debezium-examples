/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.order.saga;

import static io.debezium.examples.saga.order.saga.OrderPlacementSaga.CREDIT_APPROVAL;
import static io.debezium.examples.saga.order.saga.OrderPlacementSaga.PAYMENT;

import javax.enterprise.event.Event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.debezium.examples.saga.framework.Saga;
import io.debezium.examples.saga.framework.SagaBase;
import io.debezium.examples.saga.framework.SagaStatus;
import io.debezium.examples.saga.framework.SagaStepMessage;
import io.debezium.examples.saga.framework.internal.SagaState;
import io.debezium.examples.saga.order.event.CreditApprovalEvent;
import io.debezium.examples.saga.order.event.PaymentEvent;
import io.debezium.examples.saga.order.model.PurchaseOrder;
import io.debezium.examples.saga.order.model.PurchaseOrderStatus;
import io.debezium.outbox.quarkus.ExportedEvent;

@Saga(type="order-placement", stepIds = {CREDIT_APPROVAL, PAYMENT})
public class OrderPlacementSaga extends SagaBase {

    private static final String REQUEST = "REQUEST";
    private static final String CANCEL = "CANCEL";
    protected static final String PAYMENT = "payment";
    protected static final String CREDIT_APPROVAL = "credit-approval";

    public static JsonNode payloadFor(PurchaseOrder purchaseOrder) {
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode payload = objectMapper.createObjectNode();

        payload.put("order-id", purchaseOrder.id);
        payload.put("customer-id", purchaseOrder.customerId);
        payload.put("payment-due", purchaseOrder.paymentDue);
        payload.put("credit-card-no", purchaseOrder.creditCardNo);
        payload.put("type", REQUEST);

        return payload;
    }

    public OrderPlacementSaga(Event<ExportedEvent<?, ?>> event, SagaState state) {
        super(event, state);
    }

    @Override
    public SagaStepMessage getStepMessage(String id) {
        if (id.equals(PAYMENT)) {
            return new SagaStepMessage(PAYMENT, REQUEST, getPayload());
        }
        else {
            return new SagaStepMessage(CREDIT_APPROVAL, REQUEST, getPayload());
        }
    }

    @Override
    public SagaStepMessage getCompensatingStepMessage(String id) {
        ObjectNode payload = getPayload().deepCopy();
        payload.put("type", CANCEL);

        return new SagaStepMessage(CREDIT_APPROVAL, CANCEL, payload);
    }

    public void onPaymentEvent(PaymentEvent event) {
        if (alreadyProcessed(event.messageId)) {
            return;
        }

        onStepEvent(PAYMENT, event.status.toStepStatus());
        updateOrderStatus();

        processed(event.messageId);
    }

    public void onCreditApprovalEvent(CreditApprovalEvent event) {
        if (alreadyProcessed(event.messageId)) {
            return;
        }

        onStepEvent(CREDIT_APPROVAL, event.status.toStepStatus());
        updateOrderStatus();

        processed(event.messageId);
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
        return getPayload().get("order-id").asLong();
    }
}
