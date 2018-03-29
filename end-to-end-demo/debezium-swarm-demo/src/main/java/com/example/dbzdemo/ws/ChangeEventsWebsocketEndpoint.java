/**
 *  Copyright 2018 Gunnar Morling (http://www.gunnarmorling.de/). See
 *  the copyright.txt file in the distribution for a full listing of all
 *  contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.example.dbzdemo.ws;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/example")
@ApplicationScoped
public class ChangeEventsWebsocketEndpoint {

    Logger log = Logger.getLogger( this.getClass().getName() );

    @Inject
    WebSocketChangeEventHandler handler;

    @OnOpen
    public void open(Session session) {
        log.info( "Opening session: " + session.getId() );
        handler.getSessions().add(session);
    }

    @OnClose
    public void close(Session session, CloseReason c) {
        handler.getSessions().remove( session );
        log.info( "Closing: " + session.getId() );
    }
}
