/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphql.endpoint;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import graphql.schema.GraphQLSchema;
import graphql.servlet.GraphQLInvocationInputFactory;
import graphql.servlet.GraphQLObjectMapper;
import graphql.servlet.GraphQLQueryInvoker;
import graphql.servlet.GraphQLWebsocketServlet;

@ServerEndpoint(value = "/graphql", configurator = GraphQLWSEndpointConfigurer.class)
public class GraphQLWSEndpoint extends GraphQLWebsocketServlet {

    public GraphQLWSEndpoint(GraphQLSchema graphQLSchema) {
        super(GraphQLQueryInvoker.newBuilder().build(), GraphQLInvocationInputFactory.newBuilder(graphQLSchema).build(),
                GraphQLObjectMapper.newBuilder().build());
    }

    @Override
    @OnOpen
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        super.onOpen(session, endpointConfig);
    }

    @Override
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        super.onClose(session, closeReason);
    }

    @Override
    @OnError
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
    }
}
