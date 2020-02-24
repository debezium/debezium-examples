/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.camel.coap;

import org.apache.camel.main.Main;

public class Runner {
    private static final Main MAIN = new Main();

    public static void main(String[] args) throws Exception {
        MAIN.addRouteBuilder(CoapServer.class);
        MAIN.run();
    }
}
