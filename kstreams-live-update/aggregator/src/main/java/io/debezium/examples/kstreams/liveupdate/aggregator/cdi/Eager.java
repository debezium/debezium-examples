/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator.cdi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.FIELD,
        ElementType.TYPE,
        ElementType.METHOD,
        ElementType.PARAMETER
})
/**
 * Causes the annotated {@link ApplicationScoped} bean to be initialied eagerly.
 */
public @interface Eager {
}
