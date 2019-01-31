/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.order.rest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.debezium.examples.outbox.order.model.PurchaseOrder;
import io.debezium.examples.outbox.order.service.OrderService;

@ApplicationScoped
@Path("/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    private OrderService orderService;

    @POST
    public OrderOperationResponse addOrder(CreateOrderRequest createOrderRequest) {
        PurchaseOrder order = createOrderRequest.toOrder();
        order = orderService.addOrder(order);
        return OrderOperationResponse.from(order);
    }

    @PUT
    @Path("/{orderId}/lines/{orderLineId}")
    public OrderOperationResponse updateOrderLine(@PathParam("orderId") long orderId, @PathParam("orderLineId") long orderLineId, UpdateOrderLineRequest request) {
        PurchaseOrder updated = orderService.updateOrderLine(orderId, orderLineId, request.getNewStatus());
        return OrderOperationResponse.from(updated);
    }
}
