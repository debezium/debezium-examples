/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator.ws;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerEndpoint("/example")
@ApplicationScoped
public class ChangeEventsWebsocketEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger( ChangeEventsWebsocketEndpoint.class );

    private static final Set<Session> sessions = Collections.newSetFromMap( new ConcurrentHashMap<>() );

    @OnOpen
    public void open(Session session) {
        LOG.info( "Opening session: " + session.getId() );
        sessions.add(session);
    }

    @OnClose
    public void close(Session session, CloseReason c) {
        sessions.remove( session );
        LOG.info( "Closing: " + session.getId() );
    }

    public Set<Session> getSessions() {
        return sessions;
    }
}
