/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.cacheinvalidation.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.debezium.examples.cacheinvalidation.model.Item;

@Path("/items")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ItemResource {

    @PersistenceContext
    private EntityManager entityManager;

    @PUT
    @Transactional
    @Path("/{id}")
    public UpdateItemResponse addOrder(@PathParam("id") long id, UpdateItemRequest request) {
        Item item = entityManager.find(Item.class, id);

        if (item == null) {
            throw new NotFoundException("Item with id " + id + " doesn't exist");
        }

        UpdateItemResponse response = new UpdateItemResponse();
        response.setId(id);
        response.setOldDescription(item.getDescription());
        response.setOldPrice(item.getPrice());
        response.setNewDescription(request.getDescription());
        response.setNewPrice(request.getPrice());

        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());

        return response;
    }
}
