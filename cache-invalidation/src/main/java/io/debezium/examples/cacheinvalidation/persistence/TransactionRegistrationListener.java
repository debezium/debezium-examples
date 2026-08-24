/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.cacheinvalidation.persistence;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.enterprise.inject.spi.CDI;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.spi.TransactionCompletionCallbacks.BeforeCompletionCallback;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.query.QueryFlushMode;

/**
 * Hibernate event listener obtains the current TX id and stores it in a cache.
 *
 * @author Gunnar Morling
 */
class TransactionRegistrationListener implements FlushEventListener {

    private final ConcurrentMap<Session, Boolean> sessionsWithBeforeTransactionCompletion;

    private volatile KnownTransactions knownTransactions;

    public TransactionRegistrationListener() {
        sessionsWithBeforeTransactionCompletion = new ConcurrentHashMap<>();
    }

    @Override
    public void onFlush(FlushEvent event) throws HibernateException {
        if (sessionsWithBeforeTransactionCompletion.containsKey(event.getSession())) {
            return;
        }

        sessionsWithBeforeTransactionCompletion.put(event.getSession(), true);

        event.getSession().getActionQueue().registerCallback( (BeforeCompletionCallback) session -> {
            final var txId = session.createNativeQuery("SELECT txid_current()", Long.class)
                    .setQueryFlushMode(QueryFlushMode.NO_FLUSH)
                    .getSingleResult();

            getKnownTransactions().register(txId);

            sessionsWithBeforeTransactionCompletion.remove((Session) session);
        } );
    }

    private KnownTransactions getKnownTransactions() {
        KnownTransactions value = knownTransactions;

        if (value == null) {
            knownTransactions = value = CDI.current().select(KnownTransactions.class).get();
        }

        return value;
    }
}
