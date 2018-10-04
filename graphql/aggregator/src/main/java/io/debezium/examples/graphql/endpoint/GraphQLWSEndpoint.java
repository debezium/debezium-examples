package io.debezium.examples.graphql.endpoint;

import java.util.logging.Logger;

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

    private static final Logger LOG = Logger.getLogger(GraphQLWSEndpoint.class.getName());

    public GraphQLWSEndpoint(GraphQLSchema graphQLSchema) {
        super(GraphQLQueryInvoker.newBuilder().build(), GraphQLInvocationInputFactory.newBuilder(graphQLSchema).build(),
                GraphQLObjectMapper.newBuilder().build());
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        LOG.info(" >>>>>>>> GRAPHQL WEBSOCKET onOPEN <<<<<<<<<<<< ");
        super.onOpen(session, endpointConfig);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        LOG.info(" >>>>>>>> GRAPHQL WEBSOCKET onCLOSE <<<<<<<<<<<< ");
        super.onClose(session, closeReason);
    }

    @OnError
    public void onError(Session session, Throwable thr) {
        LOG.info(" >>>>>>>> GRAPHQL WEBSOCKET onERROR <<<<<<<<<<<< ");
        super.onError(session, thr);
    }
}
