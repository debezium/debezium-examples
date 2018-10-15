/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator.cdi;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CDI extension for starting up beans annotated with {@link ApplicationScoped} eagerly.
 */
public class EagerExtension implements Extension {

    private static final Logger LOG = LoggerFactory.getLogger( EagerExtension.class );

    private final List<Bean<?>> eagerBeansList = new ArrayList<Bean<?>>();

    public <T> void collect(@Observes ProcessBean<T> event) {
        if (event.getAnnotated().isAnnotationPresent(Eager.class) &&
                event.getAnnotated().isAnnotationPresent(ApplicationScoped.class)) {

            LOG.debug("Found an eager annotation: {}", event.getBean());

            eagerBeansList.add(event.getBean());
        }
    }

    public void load(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
         for (Bean < ? > bean : eagerBeansList) {
             LOG.debug("Eager instantiation will be performed: {}", bean.getBeanClass());
             beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)).toString();
         }
     }
}
