package io.debezium.demos.auditing.enricher;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.json.JsonObject;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TopologyProducer {

    static final String STREAM_BUFFER_NAME = "stream-buffer-state-store";
    static final String STORE_NAME = "transaction-meta-data";

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
}
