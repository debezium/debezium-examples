/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphql;

import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.coxautodev.graphql.tools.SchemaParser;

import graphql.schema.GraphQLSchema;

@ApplicationScoped
@Priority(0)
public class GraphQLConfiguration {

    private static final Logger LOG = Logger.getLogger(GraphQLConfiguration.class.getName());

    @Inject
    private RootResolver rootResolver;

    // DOES NOT WORK WITH CDI?
    // graphql-java-tool does not recognize return values from methods
    // in Subscription Resolver (failing with Publisher<...>)
    // @Inject
    // private SubscriptionResolver orderSubscriptionResolver;

    @Inject
    private OrderPublisher publisher;

    @Produces
    @Singleton
    public GraphQLSchema createGraphQL() {

        SubscriptionResolver orderSubscriptionResolver = new SubscriptionResolver(publisher);
        LOG.info("Creating GraphQL Schema...");
        final GraphQLSchema graphQLSchema = SchemaParser.newParser() //
                .file("graphql/schema.graphqls") //
                .resolvers(rootResolver, orderSubscriptionResolver) //
                .build().makeExecutableSchema();

        return graphQLSchema;

    }

}
