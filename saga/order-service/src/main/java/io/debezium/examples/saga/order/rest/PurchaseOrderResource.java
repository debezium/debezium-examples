/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.order.rest;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.debezium.examples.saga.framework.SagaManager;
import io.debezium.examples.saga.order.event.CreditApprovalEvent;
import io.debezium.examples.saga.order.event.CreditApprovalEventPayload;
import io.debezium.examples.saga.order.event.PaymentEvent;
import io.debezium.examples.saga.order.event.PaymentEventPayload;
import io.debezium.examples.saga.order.model.PurchaseOrder;
import io.debezium.examples.saga.order.saga.OrderPlacementEventHandler;
import io.debezium.examples.saga.order.saga.OrderPlacementSaga;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class PurchaseOrderResource {

    @Inject
    private SagaManager sagaManager;

    @Inject
    private OrderPlacementEventHandler eventHandler;

    @POST
    @Transactional
    public PlaceOrderResponse placeOrder(PlaceOrderRequest req) {
        PurchaseOrder order = req.toPurchaseOrder();
        order.persist();

        sagaManager.begin(OrderPlacementSaga.class, OrderPlacementSaga.payloadFor(order));

        return PlaceOrderResponse.fromPurchaseOrder(order);
    }

    @POST
    @Path("/payment")
    @Transactional
    public void onPaymentEvent(@HeaderParam("saga-id") UUID sagaId, @HeaderParam("message-id") UUID messageId, PaymentEventPayload event) {
        eventHandler.onPaymentEvent(new PaymentEvent(sagaId, messageId, event.status, null));
    }

    @POST
    @Path("/credit-approval")
    @Transactional
    public void onCreditEvent(@HeaderParam("saga-id") UUID sagaId, @HeaderParam("message-id") UUID messageId, CreditApprovalEventPayload event) {
        eventHandler.onCreditApprovalEvent(new CreditApprovalEvent(sagaId, messageId, event.status, null));
    }
}
