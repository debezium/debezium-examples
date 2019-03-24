package io.debezium.examples.nats;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Subscription;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class ChangeDataSubscriber {
    private static final String serverUrl = "nats://localhost:4222";
    private static final String subject = "NATSChannel";

    public static void main(String[] args) {
        try (Connection nc = Nats.connect(serverUrl)){
            // Subscribe a subject
            Subscription sub = nc.subscribe(subject);

            while (!Thread.interrupted()) {
                // Read a message
                Message msg = sub.nextMessage(Duration.ZERO);
                System.out.println(new String(msg.getData(), StandardCharsets.UTF_8));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
