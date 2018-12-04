package io.debezium.examples.cacheinvalidation.persistence;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps track of transactions initiated by this application, so we can tell
 * them apart from transactions initiated externally, e.g. via other DB clients.
 */
@ApplicationScoped
public class KnownTransactions {

    private static final Logger LOG = LoggerFactory.getLogger( KnownTransactions.class );

    @Inject
    @TransactionIdCache
    private Cache<Long, Boolean> applicationTransactions;

    public void register(long txId) {
        LOG.info("Registering TX {} started by this application", txId);
        applicationTransactions.put(txId, true);
    }

    public boolean isKnown(long txId) {
        return Boolean.TRUE.equals(applicationTransactions.get(txId));
    }
}
