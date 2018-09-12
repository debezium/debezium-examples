/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.aggregation.hibernate;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Integrator for Hibernate ORM that enables aggregate materialization via {@link MaterializeAggregate}.
 *
 * @author Gunnar Morling
 */
public class AggregationBuilderIntegrator implements Integrator {

    @Override
    public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory,
            SessionFactoryServiceRegistry serviceRegistry) {

        EventListenerRegistry eventListenerRegistry = serviceRegistry.getService(EventListenerRegistry.class);

        AggregationBuilderListener listener = new AggregationBuilderListener();

        eventListenerRegistry.appendListeners(EventType.POST_INSERT, listener);
        eventListenerRegistry.appendListeners(EventType.POST_UPDATE, listener);
        eventListenerRegistry.appendListeners(EventType.POST_DELETE, listener);
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
    }
}
