package org.acme;

import io.quarkus.debezium.notification.SnapshotEvent;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayDeque;
import java.util.Deque;

@ApplicationScoped
public class SnapshotService {
    private final Deque<SnapshotEvent> events = new ArrayDeque<>();

    public void send(SnapshotEvent event) {
        events.add(event);
    }

    public SnapshotEvent getLast() {
        return events.getLast();
    }
}
