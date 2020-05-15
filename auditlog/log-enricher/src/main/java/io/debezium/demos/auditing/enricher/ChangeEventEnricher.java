package io.debezium.demos.auditing.enricher;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
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
class ChangeEventEnricher implements Transformer<JsonObject, JsonObject, KeyValue<JsonObject, JsonObject>> {

    private static final Long BUFFER_OFFSETS_KEY = -1L;

    private static final Logger LOG = LoggerFactory.getLogger(ChangeEventEnricher.class);

    private ProcessorContext context;
    private TimestampedKeyValueStore<JsonObject, JsonObject> txMetaDataStore;
    private KeyValueStore<Long, JsonObject> streamBuffer;

    @Override
    @SuppressWarnings("unchecked")
    public void init(ProcessorContext context) {
        this.context = context;
        streamBuffer = (KeyValueStore<Long, JsonObject>) context.getStateStore(TopologyProducer.STREAM_BUFFER_NAME);
        txMetaDataStore = (TimestampedKeyValueStore<JsonObject, JsonObject>) context.getStateStore(TopologyProducer.STORE_NAME);

        context.schedule(Duration.ofSeconds(1), PunctuationType.WALL_CLOCK_TIME, ts -> enrichAndEmitBufferedEvents());
    }

    @Override
    public KeyValue<JsonObject, JsonObject> transform(JsonObject key, JsonObject value) {
        boolean enrichedAllBufferedEvents = enrichAndEmitBufferedEvents();

        if (!enrichedAllBufferedEvents) {
            bufferChangeEvent(key, value);
            return null;
        }

        KeyValue<JsonObject, JsonObject> enriched = enrichWithTxMetaData(key, value);
        if (enriched == null) {
            bufferChangeEvent(key, value);
        }

        return enriched;
    }

    /**
     * Enriches the buffered change event(s) with the metadata from the associated
     * transactions and forwards them.
     *
     * @return {@code true}, if all buffered events were enriched and forwarded,
     *         {@code false} otherwise.
     */
    private boolean enrichAndEmitBufferedEvents() {
        Optional<BufferOffsets> seq = bufferOffsets();

        if (!seq.isPresent()) {
            return true;
        }

        BufferOffsets sequence = seq.get();

        boolean enrichedAllBuffered = true;

        for(long i = sequence.getFirstValue(); i < sequence.getNextValue(); i++) {
            JsonObject buffered = streamBuffer.get(i);

            LOG.info("Processing buffered change event for key {}", buffered.getJsonObject("key"));

            KeyValue<JsonObject, JsonObject> enriched = enrichWithTxMetaData(buffered.getJsonObject("key"), buffered.getJsonObject("changeEvent"));
            if (enriched == null) {
                enrichedAllBuffered = false;
                break;
            }

            context.forward(enriched.key, enriched.value);
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
    private void bufferChangeEvent(JsonObject key, JsonObject changeEvent) {
        LOG.info("Buffering change event for key {}", key);

        BufferOffsets sequence = bufferOffsets().orElseGet(BufferOffsets::initial);

        JsonObject wrapper = Json.createObjectBuilder()
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
    private KeyValue<JsonObject, JsonObject> enrichWithTxMetaData(JsonObject key, JsonObject changeEvent) {
        JsonObject txId = Json.createObjectBuilder()
                .add("transaction_id", changeEvent.get("source").asJsonObject().getJsonNumber("txId").longValue())
                .build();

        ValueAndTimestamp<JsonObject> metaData = txMetaDataStore.get(txId);

        if (metaData != null) {
            LOG.info("Enriched change event for key {}", key);

            JsonObject txMetaData = Json.createObjectBuilder(metaData.value().get("after").asJsonObject())
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
        JsonObject bufferOffsets = streamBuffer.get(BUFFER_OFFSETS_KEY);
        if (bufferOffsets == null) {
            return Optional.empty();
        }
        else {
            return Optional.of(BufferOffsets.fromJson(bufferOffsets));
        }
    }

    @Override
    public void close() {
    }
}
