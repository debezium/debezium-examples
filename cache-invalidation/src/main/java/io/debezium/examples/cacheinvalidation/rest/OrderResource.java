/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.cacheinvalidation.rest;

import java.math.BigDecimal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.debezium.examples.cacheinvalidation.model.Item;
import io.debezium.examples.cacheinvalidation.model.PurchaseOrder;

@Path("/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class OrderResource {

    @PersistenceContext
    private EntityManager entityManager;

    @POST
    @Transactional
    public CreateOrderResponse addOrder(CreateOrderRequest orderRequest) {
        Item item = entityManager.find(Item.class, orderRequest.getItemId());
        PurchaseOrder po = new PurchaseOrder(
                orderRequest.getCustomer(),
                item,
                orderRequest.getQuantity(),
                item.getPrice().multiply(BigDecimal.valueOf(orderRequest.getQuantity()))
        );

        po = entityManager.merge(po);

        return new CreateOrderResponse(
                po.getId(),
                po.getCustomer(),
                po.getItem(),
                po.getQuantity(),
                po.getTotalPrice()
        );
    }
}
