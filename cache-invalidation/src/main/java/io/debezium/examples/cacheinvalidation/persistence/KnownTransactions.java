package io.debezium.examples.cacheinvalidation.persistence;

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Keeps track of transactions initiated by this application, so we can tell
 * them apart from transactions initiated externally, e.g. via other DB clients.
 */
@ApplicationScoped
public class KnownTransactions {

    private static final Logger LOG = LoggerFactory.getLogger( KnownTransactions.class );

    private Cache<Long, Boolean> applicationTransactions;

    public KnownTransactions() {
        applicationTransactions = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build();
    }

    public void register(long txId) {
        LOG.info("Registering TX {} started by this application", txId);
        applicationTransactions.put(txId, true);
    }

    public boolean isKnown(long txId) {
        return Boolean.TRUE.equals(applicationTransactions.getIfPresent(txId));
    }
}
