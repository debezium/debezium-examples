/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.order.rest;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
        eventHandler.onPaymentEvent(new PaymentEvent(sagaId, messageId, event.status));
    }

    @POST
    @Path("/credit-approval")
    @Transactional
    public void onCreditEvent(@HeaderParam("saga-id") UUID sagaId, @HeaderParam("message-id") UUID messageId, CreditApprovalEventPayload event) {
        eventHandler.onCreditApprovalEvent(new CreditApprovalEvent(sagaId, messageId, event.status));
    }
}
