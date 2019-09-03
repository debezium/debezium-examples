package io.debezium.demos.auditing.enricher;

import java.time.Duration;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.json.Json;
import javax.json.JsonObject;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TopologyProducer {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyProducer.class);

    private static final Long BUFFER_OFFSETS_KEY = -1L;
    private static final String STREAM_BUFFER_NAME = "stream-buffer-state-store";
    private static final String STORE_NAME = "transaction-meta-data";


    @ConfigProperty(name = "audit.context.data.topic")
    String txContextDataTopic;

    @ConfigProperty(name = "audit.vegetables.topic")
    String vegetablesTopic;

    @ConfigProperty(name = "audit.vegetables.enriched.topic")
    String vegetablesEnrichedTopic;

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        StoreBuilder<KeyValueStore<Long, JsonObject>> streamBufferStateStore =
                Stores
                    .keyValueStoreBuilder(
                        Stores.persistentKeyValueStore(STREAM_BUFFER_NAME),
                        new Serdes.LongSerde(),
                        new JsonObjectSerde()
                    )
                    .withCachingDisabled();
            builder.addStateStore(streamBufferStateStore);

        builder.globalTable(txContextDataTopic, Materialized.as(STORE_NAME));

        builder.<JsonObject, JsonObject>stream(vegetablesTopic)
                // filter out any tombstones; for an audit log topic, time-based retention
                // seems more reasonable than compaction
                .filter((id, changeEvent) -> changeEvent != null)
                // exclude snapshot events
                .filter((id, changeEvent) -> !changeEvent.getString("op").equals("r"))
                // enrich change events with transaction metadata via the statestore of the TX topic
                .transform(() -> new ChangeEventEnricher(), STREAM_BUFFER_NAME)
                .to(vegetablesEnrichedTopic);

        return builder.build();
    }

    /**
     * Enriches change events with transaction-scoped metadata. If no metadata for
     * the associated transaction can be retrieved yet (as the change event gets
     * processed before the corresponding transaction record), that change event
     * gets added into a buffer. Before processing the incoming change event, any
     * buffered events will be processed.
     */
    private final class ChangeEventEnricher implements Transformer<JsonObject, JsonObject, KeyValue<JsonObject, JsonObject>> {

        private ProcessorContext context;
        private KeyValueStore<JsonObject, JsonObject> txMetaDataStore;
        private KeyValueStore<Long, JsonObject> streamBuffer;

        @Override
        @SuppressWarnings("unchecked")
        public void init(ProcessorContext context) {
            this.context = context;
            streamBuffer = (KeyValueStore<Long, JsonObject>) context.getStateStore(STREAM_BUFFER_NAME);
            txMetaDataStore = (KeyValueStore<JsonObject, JsonObject>) context.getStateStore(STORE_NAME);

            context.schedule(Duration.ofSeconds(1), PunctuationType.WALL_CLOCK_TIME, ts -> {
                enrichAndEmitBufferedEvents();
            });

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

            streamBuffer.put(BUFFER_OFFSETS_KEY, sequence.toJson());

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
            streamBuffer.put(sequence.getNextValueAndIncrement(), wrapper);

            streamBuffer.put(BUFFER_OFFSETS_KEY, sequence.toJson());
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

            JsonObject metaData = txMetaDataStore.get(txId);

            if (metaData != null) {
                LOG.info("Enriched change event for key {}", key);

                metaData = Json.createObjectBuilder(metaData.get("after").asJsonObject())
                        .remove("transaction_id")
                        .build();

                return KeyValue.pair(
                        key,
                        Json.createObjectBuilder(changeEvent)
                            .add("audit", metaData)
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

    /**
     * Keeps track of the position within the key/value state store that is used as
     * a buffer for non-joinable events. As there's no true queue like structure
     * offered by Kafka Streams, a statestore is used, whose keys indicate the order
     * of insertion and whose value are the buffered change events. This structure
     * keeps track of the oldest insert position and the next insert position. It is
     * stored in the same state store using a special key outside of the sequence
     * range.
     */
    private static class BufferOffsets {

        private long firstValue;
        private long nextValue;

        private BufferOffsets(long firstValue, long nextValue) {
            this.firstValue = firstValue;
            this.nextValue = nextValue;
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
            return nextValue++;
        }

        void incrementFirstValue() {
            firstValue++;
        }

        @Override
        public String toString() {
            return "BufferOffsets [firstValue=" + firstValue + ", nextValue=" + nextValue + "]";
        }
    }
}
