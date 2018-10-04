package io.debezium.examples.graphql.endpoint;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import graphql.schema.GraphQLSchema;
import graphql.servlet.AbstractGraphQLHttpServlet;
import graphql.servlet.GraphQLInvocationInputFactory;
import graphql.servlet.GraphQLObjectMapper;
import graphql.servlet.GraphQLQueryInvoker;

@WebServlet("/graphql")
public class GraphQLServlet extends AbstractGraphQLHttpServlet {

    private static final long serialVersionUID = 1L;

    private GraphQLQueryInvoker queryInvoker;
    private GraphQLInvocationInputFactory invocationInputFactory;
    private GraphQLObjectMapper objectMapper;

    @Inject
    public GraphQLServlet(GraphQLSchema graphQLSchema) {
        this.invocationInputFactory = GraphQLInvocationInputFactory.newBuilder(graphQLSchema).build();
        this.queryInvoker = GraphQLQueryInvoker.newBuilder().build();
        this.objectMapper = GraphQLObjectMapper.newBuilder().build();
    }

    @Override
    protected GraphQLQueryInvoker getQueryInvoker() {
        return this.queryInvoker;
    }

    @Override
    protected GraphQLInvocationInputFactory getInvocationInputFactory() {
        return this.invocationInputFactory;
    }

    @Override
    protected GraphQLObjectMapper getGraphQLObjectMapper() {
        return this.objectMapper;
    }

}
