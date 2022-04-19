/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.order.rest;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.debezium.examples.caching.model.PurchaseOrder;
import io.debezium.examples.caching.order.service.OrderService;

/**
 * A resource endpoint implementation for {@link PurchaseOrder} objects.
 */
@Path("/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    OrderService orderService;

    @GET
    @Path("/{id}")
    public OrderOperationResponse getById(@PathParam("id") String orderId) {
        return orderService.getById(orderId).map(OrderOperationResponse::from)
              .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    @POST
    public OrderOperationResponse addOrder(CreateOrderRequest createOrderRequest) {
        PurchaseOrder order = createOrderRequest.toOrder();
        order = orderService.addOrder(order);
        return OrderOperationResponse.from(order);
    }

    @PUT
    @Path("/{orderId}/lines/{orderLineId}")
    public OrderOperationResponse updateOrderLine(@PathParam("orderId") long orderId, @PathParam("orderLineId") long orderLineId, UpdateOrderLineRequest request) {
        PurchaseOrder updated = orderService.updateOrderLine(orderId, request.getVersion(), orderLineId, request.getNewStatus());
        return OrderOperationResponse.from(updated);
    }
}
