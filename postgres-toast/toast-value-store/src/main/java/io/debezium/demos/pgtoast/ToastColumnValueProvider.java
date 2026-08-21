package io.debezium.demos.pgtoast;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replaces the "__debezium_unavailable_value" marker value in the
 * "products.instructions" field with values from a state store.
 */
class ToastColumnValueProvider implements FixedKeyProcessor<JsonObject, JsonObject, JsonObject> {

    private static final Logger LOG = LoggerFactory.getLogger(ToastColumnValueProvider.class);

    private FixedKeyProcessorContext<JsonObject, JsonObject> context;
    private KeyValueStore<JsonObject, String> instructionsStore;

    @Override
    @SuppressWarnings("unchecked")
    public void init(final FixedKeyProcessorContext<JsonObject, JsonObject> context) {
        this.context = context;
        instructionsStore = (KeyValueStore<JsonObject, String>) context.getStateStore(TopologyProducer.INSTRUCTIONS_STORE);
    }

    @Override
    public void process(final FixedKeyRecord<JsonObject, JsonObject> record) {
        final var key = record.key();
        var value = record.value();

        final var payload = value.getJsonObject("payload");
        final var newRowState = payload.getJsonObject("after");

        final var instructions = newRowState.getString("instructions");

        if (isUnavailableValueMarker(instructions)) {
            final var currentValue = instructionsStore.get(key);

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

        context.forward(record.withValue(value));
    }

    private boolean isUnavailableValueMarker(final String value) {
        return "__debezium_unavailable_value".contentEquals(value);
    }

    private String getBeginning(final String value) {
        return value.substring(0, Math.min(25, value.length())) + "...";
    }

    @Override
    public void close() {
    }
}
