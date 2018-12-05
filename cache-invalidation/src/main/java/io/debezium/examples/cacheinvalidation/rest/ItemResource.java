/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.cacheinvalidation.rest;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
