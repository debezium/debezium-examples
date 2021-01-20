/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.commons;

/**
 * An exception that indicates an entity could not be found.
 */
public class EntityNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -1L;

    public EntityNotFoundException(String message) {
        super(message);
    }
}
