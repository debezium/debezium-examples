/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphql.wsclient;

import java.net.URI;
import java.util.function.Consumer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleWebSocketClient extends WebSocketClient {

	private final static Logger LOG = LoggerFactory.getLogger(SimpleWebSocketClient.class);

	private final String subscriptionQuery;
	private final Consumer<String> responseHandler;

	public SimpleWebSocketClient(URI serverUri, String subscriptionQuery, Consumer<String> responseHandler) {
		super(serverUri);

		this.subscriptionQuery = subscriptionQuery;
		this.responseHandler = responseHandler;
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		LOG.debug("Sending Subscription Query to Server");

		send(subscriptionQuery);
	}

	@Override
	public void onMessage(String message) {
		LOG.debug("onMessage " + message);

		responseHandler.accept(message);
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		LOG.info("Connection closed with code {}: {} ({})", code, reason, remote);
	}

	@Override
	public void onError(Exception ex) {
		LOG.error("An error occurred: " + ex, ex);

	}

}
