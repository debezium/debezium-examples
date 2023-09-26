/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.order.rest.util;

import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.debezium.examples.caching.commons.EntityNotFoundException;

/**
 * An exception mapper for {@link EntityNotFoundException} errors.
 */
@Provider
public class OptimisticLockMapper implements ExceptionMapper<OptimisticLockException> {

    @Override
    public Response toResponse(OptimisticLockException e) {
        return Response.status(Response.Status.CONFLICT)
                .entity(e.getMessage())
                .build();
    }
}
