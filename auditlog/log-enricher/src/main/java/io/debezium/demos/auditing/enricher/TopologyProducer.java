package io.debezium.demos.auditing.enricher;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.json.Json;
import javax.json.JsonObject;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KTable;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TopologyProducer {

    @ConfigProperty(name="audit.context.data.topic")
    String txContextDataTopic;

    @ConfigProperty(name="audit.vegetables.topic")
    String vegetablesTopic;

    @ConfigProperty(name="audit.vegetables.enriched.topic")
    String vegetablesEnrichedTopic;

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        KTable<JsonObject, JsonObject> transactionContextData = builder.table(txContextDataTopic);

        builder.<JsonObject, JsonObject>stream(vegetablesTopic)
                // TODO how to pass tombstones on unmodified?
                // .filter((id, changeEvent) -> changeEvent != null)
                // store id in message value so we can restore it later on
                .map((id, changeEvent) -> KeyValue.pair(id, Json.createObjectBuilder()
                        .add("id", id)
                        .add("changeEvent", changeEvent)
                        .build())
                )
                // re-key by transaction id
                .selectKey((id, idAndChangeEvent) -> Json.createObjectBuilder()
                            .add("transaction_id", idAndChangeEvent.asJsonObject()
                                    .get("changeEvent")
                                    .asJsonObject()
                                    .get("source")
                                    .asJsonObject()
                                    .getJsonNumber("txId")
                                    .longValue())
                            .build()
                )
                // join metadata topic on TX id; add metadata into change events
                .join(
                        transactionContextData,
                        (JsonObject idAndChangeEvent, JsonObject txData) ->
                            Json.createObjectBuilder(idAndChangeEvent)
                                    .add(
                                            "changeEvent",
                                            Json.createObjectBuilder(idAndChangeEvent.get("changeEvent").asJsonObject())
                                                .add("audit", Json.createObjectBuilder(
                                                        txData.get("after")
                                                            .asJsonObject())
                                                            .remove("transaction_id")
                                                            .build()
                                                )
                                                .build())
                                    .build()
                )
                // re-key by original PK
                .selectKey((id, idAndChangeEvent) -> idAndChangeEvent.get("id").asJsonObject())
                // get rid of the temporary id + change event wrapper
                .map((id, idAndChangeEvent) ->
                    KeyValue.pair(id, idAndChangeEvent.get("changeEvent").asJsonObject())
                )
                .to(vegetablesEnrichedTopic);

        return builder.build();
    }
}
