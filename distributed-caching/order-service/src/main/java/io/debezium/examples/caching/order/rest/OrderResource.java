/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.order.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
