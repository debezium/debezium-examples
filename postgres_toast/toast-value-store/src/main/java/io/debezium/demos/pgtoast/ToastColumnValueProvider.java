package io.debezium.demos.pgtoast;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replaces the "__debezium_unavailable_value" marker value in the
 * "products.instructions" field with values from a state store.
 */
class ToastColumnValueProvider implements Transformer<JsonObject, JsonObject, KeyValue<JsonObject, JsonObject>> {

    private static final Logger LOG = LoggerFactory.getLogger(ToastColumnValueProvider.class);

    private KeyValueStore<JsonObject, String> instructionsStore;

    @Override
    @SuppressWarnings("unchecked")
    public void init(ProcessorContext context) {
        instructionsStore = (KeyValueStore<JsonObject, String>) context.getStateStore(TopologyProducer.INSTRUCTIONS_STORE);
    }

    @Override
    public KeyValue<JsonObject, JsonObject> transform(JsonObject key, JsonObject value) {
        JsonObject payload = value.getJsonObject("payload");
        JsonObject newRowState = payload.getJsonObject("after");

        String instructions = newRowState.getString("instructions");

        if (isUnavailableValueMarker(instructions)) {
            String currentValue = instructionsStore.get(key);

            if (currentValue == null) {
                LOG.warn("No instructions value found for key '{}'", key);
            }
            else {
                LOG.debug(
                        "Propagating value '{}' retrieved from state store for key '{}'",
                        getBeginning(instructions),
                        key
                );

                value = Json.createObjectBuilder(value)
                        .add(
                                "payload",
                                Json.createObjectBuilder(payload)
                                    .add(
                                            "after",
                                            Json.createObjectBuilder(newRowState).add("instructions", currentValue)
                                    )
                        )
                    .build();
            }
        }
        else {
            LOG.debug("Adding value '{}' to state store for key '{}'", getBeginning(instructions), key);
            instructionsStore.put(key, instructions);
        }

        return KeyValue.pair(key, value);
    }

    private boolean isUnavailableValueMarker(String instructions) {
        return "__debezium_unavailable_value".contentEquals(instructions);
    }

    private String getBeginning(String value) {
        return value.substring(0, Math.min(25, value.length())) + "...";
    }

    @Override
    public void close() {
    }
}
