package io.debezium.example.saga.order.rest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.debezium.example.saga.framework.SagaManager;
import io.debezium.example.saga.order.model.PurchaseOrder;
import io.debezium.example.saga.order.saga.MyOrderPlacementSaga;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class PurchaseOrderResource {

    @Inject
    private SagaManager sagaManager;

    @Inject
    private EntityManager entityManager;

    @POST
    @Transactional
    public PlaceOrderResponse placeOrder(PlaceOrderRequest req) {
        PurchaseOrder order = req.toPurchaseOrder();
        order.persist();

        sagaManager.begin(new MyOrderPlacementSaga(order));
//
//        OrderPlacementSaga saga = OrderPlacementSaga.forOrder(order);
//        entityManager.persist(saga);
//
//        OrderPlacementSagaEvent paymentRequest = new OrderPlacementSagaEvent();
//        paymentRequest.saga = saga;
//        paymentRequest.type = "payment-request";
//        paymentRequest.payload = "{ \"sagaId\" : " + saga.getId() + ", \"amountDue\" : " + req.paymentDue + "}";
//        entityManager.persist(paymentRequest);
//
//        OrderPlacementSagaEvent creditRequest = new OrderPlacementSagaEvent();
//        creditRequest.saga = saga;
//        creditRequest.type = "credit-request";
//        creditRequest.payload = "{ \"sagaId\" : " + saga.getId() + ", \"amountDue\" : " + req.paymentDue + "}";
//        entityManager.persist(creditRequest);

        return PlaceOrderResponse.fromPurchaseOrder(order);
    }
}
