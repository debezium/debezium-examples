/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.signal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.debezium.config.CommonConnectorConfig;
import io.debezium.pipeline.signal.SignalRecord;
import io.debezium.pipeline.signal.channels.SignalChannelReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpSignalChannel implements SignalChannelReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpSignalChannel.class);
    public static final String CHANNEL_NAME = "http";
    private static final List<SignalRecord> SIGNALS = new ArrayList<>();
    private static int COUNTER = 0;
    public CommonConnectorConfig connectorConfig;

    @Override
    public String name() {
        return CHANNEL_NAME;
    }

    @Override
    public void init(CommonConnectorConfig connectorConfig) {
        this.connectorConfig = connectorConfig;
    }

    @Override
    public List<SignalRecord> read() {
        while (COUNTER <1) {
            LOGGER.info("Reading signaling events from http endpoint");
            try {
                String requestUrl = "http://mockServer:1080/api/signal?code=10969";

                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(requestUrl))
                        .GET()
                        .header("Content-Type", "application/json")
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    String responseBody = response.body();

                    JsonNode signalJson = mapper.readTree(responseBody);
                    Map<String, Object> additionalData = signalJson.has("additionalData") ? mapper.convertValue(signalJson.get("additionalData"), new TypeReference<>() {}) : new HashMap<>();
                    String id = signalJson.get("id").asText();
                    String type = signalJson.get("type").asText();
                    String data = signalJson.get("data").toString();
                    SignalRecord signal = new SignalRecord(id, type, data, additionalData);

                    LOGGER.info("Recorded signal event '{}' ", signal);
                    SIGNALS.add(signal);
                    COUNTER++;
                } else {
                    LOGGER.warn("Error while reading signaling events from endpoint: {}", response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.warn("Exception while preparing to process the signal '{}' from the endpoint", e.getMessage());
                e.printStackTrace();
            }
        }
        return SIGNALS;
    }

    @Override
    public void close() {
        SIGNALS.clear();
    }
}
