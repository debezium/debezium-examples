/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.eventsource;

public class Main {

    private void run() {
        EventSource source = new EventSource();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping...");
            source.stop();
        }));

        source.run();
    }

    public static void main(String[] args) {
        new Main().run();
    }
}
