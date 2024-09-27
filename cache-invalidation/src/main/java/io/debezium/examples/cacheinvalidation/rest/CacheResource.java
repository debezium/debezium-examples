/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.cacheinvalidation.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.debezium.examples.cacheinvalidation.model.Item;

@Path("/cache")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CacheResource {

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    @DELETE
    @Path("/item/{id}")
    public void invalidateItemCacheEntry(@PathParam("id") long itemId) {
        entityManagerFactory.getCache().evict(Item.class, itemId);
    }

    @GET
    @Path("/item/{id}")
    public boolean isContained(@PathParam("id") long itemId) {
        return entityManagerFactory.getCache().contains(Item.class, itemId);
    }
}
