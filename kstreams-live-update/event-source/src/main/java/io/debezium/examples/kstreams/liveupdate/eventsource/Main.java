/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.eventsource;

public class Main {

    private void run() {
        String databaseServer = System.getenv("DATABASE_SERVER");
        if (databaseServer == null) {
            /* backwards compatibility */
            databaseServer = "mysql";
        }
        EventSource source = new EventSource(databaseServer);

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
