/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphql.wsclient;

import java.net.URI;

public class GraphQLSubscriptionClient {

	public static void runSubscription(String uri, String subscriptionQuery, GraphQLResponseReceiver receiver) {
		try {
			String graphqlMessage = JSONConverter.instance().toJSON(new GraphQLQueryRequest(subscriptionQuery));

			SimpleWebSocketClient client = new SimpleWebSocketClient(new URI(uri), graphqlMessage, msg -> {

				try {
					GraphQLQueryResponse response = JSONConverter.instance().toObject(msg, GraphQLQueryResponse.class);
					receiver.messageReceived(response);
				} catch (Exception e) {
					throw new RuntimeException("Could not convert response from server: " + e, e);
				}

			});

			client.connect();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
