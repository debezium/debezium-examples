package io.debezium.example.saga.order.rest;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.debezium.example.saga.framework.SagaManager;
import io.debezium.example.saga.framework.SagaStepState;
import io.debezium.example.saga.order.saga.MyOrderPlacementSaga;
import io.debezium.example.saga.order.saga.OrderPlacementSaga;
import io.debezium.example.saga.order.saga.SagaStatus;
import io.debezium.example.saga.order.saga.SagaStepStatus;

@Path("/sagas/purchase-order")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PurchaseOrderSagaResource2 {

    @Inject
    private SagaManager sagaManager;

    @Inject
    private EntityManager entityManager;

    @POST
    @Path("/payment")
    @Transactional
    public void onPaymentEvent(PaymentStatusEvent event) {
        MyOrderPlacementSaga saga1 = sagaManager.find(MyOrderPlacementSaga.class, event.sagaId);

        SagaStepState stepState = saga1.onPaymentEvent(event);
        sagaManager.process(saga1, stepState);

//        OrderPlacementSaga saga = entityManager.find(OrderPlacementSaga.class, event.sagaId);
//
//        OrderPlacementSagaEventHandler handler = OrderPlacementSagaEventHandler.forSagaStatus(saga.getStatus());
//        handler.onPaymentEvent(saga, event);
//
//        if (event.status == PaymentStatus.FAILED) {
//            if (saga.getCreditApprovalStatus() == SagaStepStatus.STARTED || saga.getCreditApprovalStatus() == SagaStepStatus.SUCCEEDED) {
//                OrderPlacementSagaEvent creditRequest = new OrderPlacementSagaEvent();
//                creditRequest.saga = saga;
//                creditRequest.type = "credit-request";
//                creditRequest.payload = "{ \"sagaId\" : " + saga.getId() + ", \"aborted\" : true }";
//                saga.setPaymentStatus(SagaStepStatus.ABORTING);
//                entityManager.persist(creditRequest);
//            }
//        }
    }

    @POST
    @Path("/credit-approval")
    @Transactional
    public void onCreditEvent(CreditApprovalStatusEvent event) {
        MyOrderPlacementSaga saga1 = sagaManager.find(MyOrderPlacementSaga.class, event.sagaId);
        SagaStepState stepState = saga1.onCreditApprovalEvent(event);
        sagaManager.process(saga1, stepState);


//        OrderPlacementSaga saga = entityManager.find(OrderPlacementSaga.class, event.sagaId);



//        OrderPlacementSagaEventHandler handler = OrderPlacementSagaEventHandler.forSagaStatus(saga.getStatus());
//        handler.onCreditEvent(saga, event);
//
//        if (event.status == CreditApprovalStatus.FAILED) {
//            if (saga.getPaymentStatus() == SagaStepStatus.STARTED || saga.getPaymentStatus() == SagaStepStatus.SUCCEEDED) {
//                OrderPlacementSagaEvent paymentRequest = new OrderPlacementSagaEvent();
//                paymentRequest.saga = saga;
//                paymentRequest.type = "payment-request";
//                paymentRequest.payload = "{ \"sagaId\" : " + saga.getId() + ", \"aborted\" : true }";
//                saga.setPaymentStatus(SagaStepStatus.ABORTING);
//                entityManager.persist(paymentRequest);
//            }
//        }
    }

    private enum OrderPlacementSagaEventHandler {

        STARTED {

            @Override
            void onPaymentEvent(OrderPlacementSaga saga, PaymentStatusEvent event) {
                if (event.status == PaymentStatus.SUCCEEDED) {
                    saga.setPaymentStatus(SagaStepStatus.SUCCEEDED);
                }
                else if (event.status == PaymentStatus.ABORTED) {
                    saga.setPaymentStatus(SagaStepStatus.ABORTED);
                }
                else if (event.status == PaymentStatus.FAILED) {
                    saga.setPaymentStatus(SagaStepStatus.FAILED);
                }
            }

            @Override
            void onCreditEvent(OrderPlacementSaga saga, CreditApprovalStatusEvent event) {
                if (event.status == CreditApprovalStatus.SUCCEEDED) {
                    saga.setCreditApprovalStatus(SagaStepStatus.SUCCEEDED);
                }
                else if (event.status == CreditApprovalStatus.ABORTED) {
                    saga.setCreditApprovalStatus(SagaStepStatus.ABORTED);
                }
                else if (event.status == CreditApprovalStatus.FAILED) {
                    saga.setCreditApprovalStatus(SagaStepStatus.FAILED);
                }
            }

        },
        COMPLETED {

            @Override
            void onPaymentEvent(OrderPlacementSaga saga, PaymentStatusEvent event) {
            }

            @Override
            void onCreditEvent(OrderPlacementSaga saga, CreditApprovalStatusEvent event) {
            }
        };

        public static OrderPlacementSagaEventHandler forSagaStatus(SagaStatus status) {
            return OrderPlacementSagaEventHandler.STARTED;
//            switch (status) {
//            case STARTED: return OrderPlacementSagaEventHandler.STARTED;
//            default:
//                throw new IllegalArgumentException();
//            }
        }

        abstract void onPaymentEvent(OrderPlacementSaga saga, PaymentStatusEvent event);

        abstract void onCreditEvent(OrderPlacementSaga saga, CreditApprovalStatusEvent event);
    }
}
