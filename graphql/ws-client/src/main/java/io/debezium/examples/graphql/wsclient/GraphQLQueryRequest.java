package io.debezium.examples.graphql.wsclient;

/**
 * Represents a GraphQL HTTP Request payload
 *
 * @see https://graphql.org/learn/serving-over-http/#post-request
 */
public class GraphQLQueryRequest {

	private final String query;

	public GraphQLQueryRequest(String query) {
		this.query = query;
	}

	public String getQuery() {
		return query;
	}
}
