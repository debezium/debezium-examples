package io.debezium.examples.saga.order.saga;

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
    public void onPaymentEvent(PaymentEvent event) {
        OrderPlacementSaga saga = sagaManager.find(OrderPlacementSaga.class, event.sagaId);

        if (saga == null) {
            return;
        }

        saga.onPaymentEvent(event);
    }

    @Transactional
    public void onCreditApprovalEvent(CreditApprovalEvent event) {
        OrderPlacementSaga saga = sagaManager.find(OrderPlacementSaga.class, event.sagaId);

        if (saga == null) {
            return;
        }

        saga.onCreditApprovalEvent(event);
    }
}
