package io.debezium.example.saga.order.saga;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.example.saga.framework.Saga;
import io.debezium.example.saga.framework.SagaStepEvent;
import io.debezium.example.saga.framework.SagaStepState;
import io.debezium.example.saga.order.model.PurchaseOrder;
import io.debezium.example.saga.order.rest.CreditApprovalStatus;
import io.debezium.example.saga.order.rest.CreditApprovalStatusEvent;
import io.debezium.example.saga.order.rest.PaymentStatus;
import io.debezium.example.saga.order.rest.PaymentStatusEvent;

public class MyOrderPlacementSaga implements Saga {

    private static ObjectMapper objectMapper = new ObjectMapper();

    private UUID id;
    private String payload;

    public MyOrderPlacementSaga(UUID id, String payload) {
        this.id = id;
        this.payload = payload;
    }

    public MyOrderPlacementSaga(PurchaseOrder purchaseOrder) {
        id = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>();
        payload.put("customer-id", purchaseOrder.customerId);
        payload.put("payment-due", purchaseOrder.paymentDue);
        payload.put("credit-card-no", purchaseOrder.creditCardNo);

        try {
            this.payload = objectMapper.writeValueAsString(payload);
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
        return new HashSet<>(Arrays.asList("credit-approval", "payment"));
    }

    @Override
    public SagaStepEvent getStepEvent(String id) {
        if (id.equals("payment")) {
            return new SagaStepEvent("payment", payload);
        }
        else {
            return new SagaStepEvent("credit-approval", payload);
        }
    }

    @Override
    public SagaStepEvent getCompensatingStep(String id) {
        if (id.equals("payment")) {
            return new SagaStepEvent("payment", "{ \"aborted\" : true }");
        }
        else {
            return new SagaStepEvent("credit-approval", "{ \"aborted\" : true }");
        }
    }

    public SagaStepState onPaymentEvent(PaymentStatusEvent event) {
        return new SagaStepState(
                "payment",
                event.status == PaymentStatus.ABORTED ? SagaStepStatus.ABORTED : event.status == PaymentStatus.FAILED ? SagaStepStatus.FAILED : SagaStepStatus.SUCCEEDED
        );
    }

    public SagaStepState onCreditApprovalEvent(CreditApprovalStatusEvent event) {
        return new SagaStepState(
                "credit-approval",
                event.status == CreditApprovalStatus.ABORTED ? SagaStepStatus.ABORTED : event.status == CreditApprovalStatus.FAILED ? SagaStepStatus.FAILED : SagaStepStatus.SUCCEEDED
        );
    }
}
