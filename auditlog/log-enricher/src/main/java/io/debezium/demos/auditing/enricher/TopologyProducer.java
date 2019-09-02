package io.debezium.demos.auditing.enricher;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.json.Json;
import javax.json.JsonObject;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TopologyProducer {

    private static final String STORE_NAME = "transaction-meta-data";

    private static final Logger LOG = LoggerFactory.getLogger(TopologyProducer.class);

    @ConfigProperty(name = "audit.context.data.topic")
    String txContextDataTopic;

    @ConfigProperty(name = "audit.vegetables.topic")
    String vegetablesTopic;

    @ConfigProperty(name = "audit.vegetables.enriched.topic")
    String vegetablesEnrichedTopic;

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        builder.globalTable(txContextDataTopic, Materialized.as(STORE_NAME));

        builder.<JsonObject, JsonObject>stream(vegetablesTopic)
                // filter out any tombstones; for an audit log topic, time-based retention
                // seems more reasonable than compaction
                .filter((id, changeEvent) -> changeEvent != null)

                // enrich change events with transaction metadata via the statestore of the TX
                // topic
                .transform(() -> new ChangeEventEnricher())
                .to(vegetablesEnrichedTopic);

        return builder.build();
    }

    private final class ChangeEventEnricher implements Transformer<JsonObject, JsonObject, KeyValue<JsonObject, JsonObject>> {

        private KeyValueStore<JsonObject, JsonObject> txMetaDataStore;

        @Override
        @SuppressWarnings("unchecked")
        public void init(ProcessorContext context) {
            txMetaDataStore = (KeyValueStore<JsonObject, JsonObject>) context.getStateStore(STORE_NAME);
        }

        @Override
        public KeyValue<JsonObject, JsonObject> transform(JsonObject key, JsonObject value) {
            JsonObject txId = Json.createObjectBuilder()
                    .add("transaction_id", value.get("source").asJsonObject().getJsonNumber("txId").longValue())
                    .build();

            JsonObject metaData = getTransactionMetaData(txId);

            if (metaData != null) {
                value = Json.createObjectBuilder(value).add("audit", metaData).build();
            }

            return KeyValue.pair(key, value);
        }

        @Override
        public void close() {
        }

        private JsonObject getTransactionMetaData(JsonObject txId) {
            int i = 1;

            while (i < 1_000) {
                JsonObject metadata = txMetaDataStore.get(txId);

                if (metadata != null) {
                    LOG.info("Found TX metadata after {} attempt(s)", i);
                    return Json.createObjectBuilder(metadata.get("after").asJsonObject())
                            .remove("transaction_id")
                            .build();
                }
                i++;

                try {
                    Thread.sleep(1);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            return null;
        }
    }
}
