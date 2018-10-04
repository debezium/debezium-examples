package io.debezium.examples.graphql.endpoint;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import graphql.schema.GraphQLSchema;

@Singleton
public class GraphQLWSEndpointConfigurer extends ServerEndpointConfig.Configurator {

    private GraphQLWSEndpoint endpoint;

    @Inject
    public GraphQLWSEndpointConfigurer(GraphQLSchema graphQLSchema) {
        endpoint = new GraphQLWSEndpoint(graphQLSchema);
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        endpoint.modifyHandshake(sec, request, response);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        return (T) this.endpoint;
    }

}
