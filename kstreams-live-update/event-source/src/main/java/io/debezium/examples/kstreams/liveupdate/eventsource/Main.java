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
