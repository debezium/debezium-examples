package io.debezium.examples.camel.pipeline;

import org.apache.camel.main.Main;

public class Runner {
    private static final Main MAIN = new Main();

    public static void main(String[] args) throws Exception {
        MAIN.addRouteBuilder(QaDatabaseUserNotifier.class);
        MAIN.run();
    }
}
