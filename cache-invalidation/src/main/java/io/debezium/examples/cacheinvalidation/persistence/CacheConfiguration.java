/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.cacheinvalidation.persistence;
import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.Produces;

import org.infinispan.cdi.embedded.ConfigureCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;

public class CacheConfiguration {

    @ConfigureCache("tx-id-cache")
    @TransactionIdCache
    @Produces
    public Configuration greetingCacheConfiguration() {
        return new ConfigurationBuilder()
                    .expiration()
                        .lifespan(60, TimeUnit.SECONDS)
                    .build();
    }
}
