/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphql;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;

import io.debezium.examples.graphql.model.Order;
import io.reactivex.Maybe;

@ApplicationScoped
public class RootResolver implements GraphQLQueryResolver {

    @Inject
    private OrderPublisher publisher;

    public String hello() {
        return "GraphQL API is alive";
    }

    public Order latestOrder() {
        Maybe<Order> first = publisher.getPublisher().firstElement();
        return first.blockingGet();
    }
}
