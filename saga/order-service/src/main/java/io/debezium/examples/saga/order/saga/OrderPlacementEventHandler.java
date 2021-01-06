package io.debezium.examples.saga.order.saga;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import io.debezium.examples.saga.framework.SagaManager;
import io.debezium.examples.saga.order.event.CreditApprovalEvent;
import io.debezium.examples.saga.order.event.PaymentEvent;

@ApplicationScoped
public class OrderPlacementEventHandler {

    @Inject
    private SagaManager sagaManager;

    @Transactional
    public void onPaymentEvent(UUID sagaId, UUID messageId, PaymentEvent event) {
        OrderPlacementSaga saga = sagaManager.find(OrderPlacementSaga.class, sagaId);
        saga.onPaymentEvent(messageId, event);
    }

    @Transactional
    public void onCreditApprovalEvent(UUID sagaId, UUID messageId, CreditApprovalEvent event) {
        OrderPlacementSaga saga = sagaManager.find(OrderPlacementSaga.class, sagaId);
        saga.onCreditApprovalEvent(messageId, event);
    }
}
