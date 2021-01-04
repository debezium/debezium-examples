/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.example.saga.order.rest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.debezium.example.saga.framework.SagaManager;
import io.debezium.example.saga.order.model.PurchaseOrder;
import io.debezium.example.saga.order.saga.OrderPlacementSaga;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class PurchaseOrderResource {

    @Inject
    private SagaManager sagaManager;

    @POST
    @Transactional
    public PlaceOrderResponse placeOrder(PlaceOrderRequest req) {
        PurchaseOrder order = req.toPurchaseOrder();
        order.persist();

        sagaManager.begin(OrderPlacementSaga.forPurchaseOrder(order));

        return PlaceOrderResponse.fromPurchaseOrder(order);
    }

    @POST
    @Path("/payment")
    @Transactional
    public void onPaymentEvent(PaymentEvent event) {
        OrderPlacementSaga saga = sagaManager.find(OrderPlacementSaga.class, event.sagaId);
        saga.onPaymentEvent(event);
    }

    @POST
    @Path("/credit-approval")
    @Transactional
    public void onCreditEvent(CreditApprovalEvent event) {
        OrderPlacementSaga saga = sagaManager.find(OrderPlacementSaga.class, event.sagaId);
        saga.onCreditApprovalEvent(event);
    }
}
