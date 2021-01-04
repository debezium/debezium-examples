package io.debezium.example.saga.order.rest;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.debezium.example.saga.framework.SagaManager;
import io.debezium.example.saga.framework.SagaStepState;
import io.debezium.example.saga.order.saga.MyOrderPlacementSaga;

@Path("/sagas/purchase-order")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PurchaseOrderSagaResource {

    @Inject
    private SagaManager sagaManager;

    @POST
    @Path("/payment")
    @Transactional
    public void onPaymentEvent(PaymentStatusEvent event) {
        MyOrderPlacementSaga saga = sagaManager.find(MyOrderPlacementSaga.class, event.sagaId);
        SagaStepState stepState = saga.onPaymentEvent(event);
        sagaManager.process(saga, stepState);
    }

    @POST
    @Path("/credit-approval")
    @Transactional
    public void onCreditEvent(CreditApprovalStatusEvent event) {
        MyOrderPlacementSaga saga = sagaManager.find(MyOrderPlacementSaga.class, event.sagaId);
        SagaStepState stepState = saga.onCreditApprovalEvent(event);
        sagaManager.process(saga, stepState);
    }
}
