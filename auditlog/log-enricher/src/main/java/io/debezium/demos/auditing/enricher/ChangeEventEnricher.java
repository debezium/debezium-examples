package io.debezium.demos.auditing.enricher;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.TimestampedKeyValueStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enriches change events with transaction-scoped metadata. If no metadata for
 * the associated transaction can be retrieved yet (as the change event gets
 * processed before the corresponding transaction record), that change event
 * gets added into a buffer. Before processing the incoming change event, any
 * buffered events will be processed.
 */
class ChangeEventEnricher extends ContextualProcessor<JsonObject, JsonObject, JsonObject, JsonObject> {

    private static final Long BUFFER_OFFSETS_KEY = -1L;

    private static final Logger LOG = LoggerFactory.getLogger(ChangeEventEnricher.class);

    private TimestampedKeyValueStore<JsonObject, JsonObject> txMetaDataStore;
    private KeyValueStore<Long, JsonObject> streamBuffer;

    @Override
    @SuppressWarnings("unchecked")
    public void init(org.apache.kafka.streams.processor.api.ProcessorContext<JsonObject, JsonObject> context) {
        super.init(context);
        streamBuffer = (KeyValueStore<Long, JsonObject>) context.getStateStore(TopologyProducer.STREAM_BUFFER_NAME);
        txMetaDataStore = (TimestampedKeyValueStore<JsonObject, JsonObject>) context.getStateStore(TopologyProducer.STORE_NAME);

        context.schedule(Duration.ofSeconds(1), PunctuationType.WALL_CLOCK_TIME, ts -> enrichAndEmitBufferedEvents());
    }

    @Override
    public void process(Record<JsonObject, JsonObject> record) {
        final var key = record.key();
        final var value = record.value();

        final var enrichedAllBufferedEvents = enrichAndEmitBufferedEvents();

        if (!enrichedAllBufferedEvents) {
            bufferChangeEvent(key, value);
            return;
        }

        final var enriched = enrichWithTxMetaData(key, value);
        if (enriched == null) {
            bufferChangeEvent(key, value);
        }
        else {
            context().forward(record.withKey(enriched.key).withValue(enriched.value));
        }
    }

    /**
     * Enriches the buffered change event(s) with the metadata from the associated
     * transactions and forwards them.
     *
     * @return {@code true}, if all buffered events were enriched and forwarded,
     *         {@code false} otherwise.
     */
    private boolean enrichAndEmitBufferedEvents() {
        final var seq = bufferOffsets();

        if (!seq.isPresent()) {
            return true;
        }

        final var sequence = seq.get();

        var enrichedAllBuffered = true;

        for (long i = sequence.getFirstValue(); i < sequence.getNextValue(); i++) {
            final var buffered = streamBuffer.get(i);

            LOG.info("Processing buffered change event for key {}", buffered.getJsonObject("key"));

            final var enriched = enrichWithTxMetaData(buffered.getJsonObject("key"), buffered.getJsonObject("changeEvent"));
            if (enriched == null) {
                enrichedAllBuffered = false;
                break;
            }

            context().forward(new Record<>(enriched.key, enriched.value, System.currentTimeMillis()));
            streamBuffer.delete(i);
            sequence.incrementFirstValue();
        }

        if (sequence.isModified()) {
            streamBuffer.put(BUFFER_OFFSETS_KEY, sequence.toJson());
        }

        return enrichedAllBuffered;
    }

    /**
     * Adds the given change event to the stream-side buffer.
     */
    private void bufferChangeEvent(final JsonObject key, final JsonObject changeEvent) {
        LOG.info("Buffering change event for key {}", key);

        final var sequence = bufferOffsets().orElseGet(BufferOffsets::initial);

        final var wrapper = Json.createObjectBuilder()
                .add("key", key)
                .add("changeEvent", changeEvent)
                .build();

        streamBuffer.putAll(Arrays.asList(
                KeyValue.pair(sequence.getNextValueAndIncrement(), wrapper),
                KeyValue.pair(BUFFER_OFFSETS_KEY, sequence.toJson())
        ));
    }

    /**
     * Enriches the given change event with the metadata from the associated
     * transaction.
     *
     * @return The enriched change event or {@code null} if no metadata for the
     *         associated transaction was found.
     */
    private KeyValue<JsonObject, JsonObject> enrichWithTxMetaData(final JsonObject key, final JsonObject changeEvent) {
        final var txId = Json.createObjectBuilder()
                .add("transaction_id", changeEvent.get("source").asJsonObject().getJsonNumber("txId").longValue())
                .build();

        final ValueAndTimestamp<JsonObject> metaData = txMetaDataStore.get(txId);

        if (metaData != null) {
            LOG.info("Enriched change event for key {}", key);

            final var txMetaData = Json.createObjectBuilder(metaData.value().get("after").asJsonObject())
                    .remove("transaction_id")
                    .build();

            return KeyValue.pair(
                    key,
                    Json.createObjectBuilder(changeEvent)
                        .add("audit", txMetaData)
                        .build()
            );
        }

        LOG.warn("No metadata found for transaction {}", txId);
        return null;
    }

    private Optional<BufferOffsets> bufferOffsets() {
        final var bufferOffsets = streamBuffer.get(BUFFER_OFFSETS_KEY);
        if (bufferOffsets == null) {
            return Optional.empty();
        }
        else {
            return Optional.of(BufferOffsets.fromJson(bufferOffsets));
        }
    }
}
