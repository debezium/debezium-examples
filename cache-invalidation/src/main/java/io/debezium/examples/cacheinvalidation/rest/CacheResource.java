/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.cacheinvalidation.rest;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
