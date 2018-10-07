package io.debezium.examples.graphql.wsclient;

@FunctionalInterface
public interface GraphQLResponseReceiver {
	void messageReceived(GraphQLQueryResponse s);
}
