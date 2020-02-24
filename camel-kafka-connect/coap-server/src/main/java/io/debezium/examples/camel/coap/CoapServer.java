/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.camel.coap;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

public class CoapServer extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("coap:0.0.0.0:{{coap.port}}/data")
                .log(LoggingLevel.INFO, "CoAP server has received message '${body}' with headers ${headers}");
    }
}
