package io.debezium.examples.cacheinvalidation.persistence;

import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps track of transactions initiated by this application, so we can tell
 * them apart from transactions initiated externally, e.g. via other DB clients.
 */
@ApplicationScoped
public class KnownTransactions {

    private static final Logger LOG = LoggerFactory.getLogger( KnownTransactions.class );

    private final DefaultCacheManager cacheManager;
    private final Cache<Long, Boolean> applicationTransactions;

    public KnownTransactions() {
        cacheManager = new DefaultCacheManager();
        cacheManager.defineConfiguration(
                "tx-id-cache",
                new ConfigurationBuilder()
                    .simpleCache(true)
                    .expiration()
                        .lifespan(60, TimeUnit.SECONDS)
                    .build()
                );

        applicationTransactions = cacheManager.getCache("tx-id-cache");
    }

    @PreDestroy
    public void stopCacheManager() {
        cacheManager.stop();
    }

    public void register(long txId) {
        LOG.info("Registering TX {} started by this application", txId);
        applicationTransactions.put(txId, true);
    }

    public boolean isKnown(long txId) {
        return Boolean.TRUE.equals(applicationTransactions.get(txId));
    }
}
