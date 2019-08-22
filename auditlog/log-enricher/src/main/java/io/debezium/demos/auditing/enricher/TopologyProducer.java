package io.debezium.demos.auditing.enricher;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.json.Json;
import javax.json.JsonObject;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KTable;

@ApplicationScoped
public class TopologyProducer {

    private static final String TX_CONTEXT_DATA_TOPIC = "dbserver1.inventory.transaction_context_data";
    private static final String VEGETABLES_TOPIC = "dbserver1.inventory.vegetable";
    private static final String VEGETABLES_ENRICHED_TOPIC = "dbserver1.inventory.vegetable.enriched";

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        KTable<JsonObject, JsonObject> transactionContextData = builder.table(TX_CONTEXT_DATA_TOPIC);

        builder.<JsonObject, JsonObject>stream(VEGETABLES_TOPIC)
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
                .to(VEGETABLES_ENRICHED_TOPIC);

        return builder.build();
    }
}
