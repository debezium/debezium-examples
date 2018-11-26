/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.cacheinvalidation.rest;

import java.math.BigDecimal;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
        PurchaseOrder po = new PurchaseOrder(orderRequest.getCustomer(), item, orderRequest.getQuantity());
        po = entityManager.merge(po);

        return new CreateOrderResponse(
                po.getId(),
                po.getCustomer(),
                po.getItem(),
                po.getQuantity(),
                item.getPrice().multiply(BigDecimal.valueOf(po.getQuantity()))
        );
    }
}
