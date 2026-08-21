package io.debezium.demos.pgtoast;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.json.JsonObject;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TopologyProducer {

    static final String INSTRUCTIONS_STORE = "instructions-store";

    @ConfigProperty(name = "pgtoast.products.topic")
    String productsTopic;

    @ConfigProperty(name = "pgtoast.products.enriched.topic")
    String productsEnrichedTopic;

    @Produces
    public Topology buildTopology() {
        final var builder = new StreamsBuilder();

        final StoreBuilder<KeyValueStore<JsonObject, String>> instructionsStore =
                Stores.keyValueStoreBuilder(
                    Stores.persistentKeyValueStore(INSTRUCTIONS_STORE),
                    new JsonObjectSerde(),
                    new Serdes.StringSerde()
                );
        builder.addStateStore(instructionsStore);

        builder.<JsonObject, JsonObject>stream(productsTopic)
                .processValues(ToastColumnValueProvider::new, INSTRUCTIONS_STORE)
                .to(productsEnrichedTopic);

        return builder.build();
    }
}
