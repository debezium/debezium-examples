/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.cacheinvalidation.persistence;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integrator for Hibernate ORM that registers the {@link TransactionRegistrationListener}.
 *
 * @author Gunnar Morling
 */
public class TransactionRegistrationIntegrator implements Integrator {

    private static final Logger LOG = LoggerFactory.getLogger( TransactionRegistrationIntegrator.class );


    @Override
    public void integrate(Metadata metadata, BootstrapContext bootstrapContext,
                          SessionFactoryImplementor sessionFactory) {
        LOG.info("TransactionRegistrationIntegrator#integrate()");

        sessionFactory.getEventListenerRegistry()
                .appendListeners(EventType.FLUSH, new TransactionRegistrationListener());
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
    }
}
