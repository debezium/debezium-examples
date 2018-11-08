/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphql.wsclient;

@FunctionalInterface
public interface GraphQLResponseReceiver {
	void messageReceived(GraphQLQueryResponse s);
}
