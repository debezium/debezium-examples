package io.debezium.demos.auditing.enricher;

import jakarta.json.Json;
import jakarta.json.JsonObject;

/**
 * Keeps track of the position within the key/value state store that is used as
 * a buffer for non-joinable events. As there's no true queue like structure
 * offered by Kafka Streams, a statestore is used, whose keys indicate the order
 * of insertion and whose value are the buffered change events. This structure
 * keeps track of the oldest insert position and the next insert position. It is
 * stored in the same state store using a special key outside of the sequence
 * range.
 */
class BufferOffsets {

    private long firstValue;
    private long nextValue;
    private boolean modified;

    private BufferOffsets(long firstValue, long nextValue) {
        this.firstValue = firstValue;
        this.nextValue = nextValue;
        this.modified = false;
    }

    public static BufferOffsets initial() {
        return new BufferOffsets(1, 1);
    }

    static BufferOffsets fromJson(JsonObject json) {
        return new BufferOffsets(
                json.getJsonNumber("firstValue").longValue(),
                json.getJsonNumber("nextValue").longValue()
        );
    }

    public JsonObject toJson() {
        return Json.createObjectBuilder()
                .add("firstValue", firstValue)
                .add("nextValue", nextValue)
                .build();
    }

    public long getNextValue() {
        return nextValue;
    }

    long getFirstValue() {
        return firstValue;
    }

    long getNextValueAndIncrement() {
        modified = true;
        return nextValue++;
    }

    void incrementFirstValue() {
        modified = true;
        firstValue++;
    }

    public boolean isModified() {
        return modified;
    }

    @Override
    public String toString() {
        return "BufferOffsets [firstValue=" + firstValue + ", nextValue=" + nextValue + ", modified=" + modified + "]";
    }
}
