/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.cacheupdater.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.debezium.examples.caching.commons.OrderLine;
import io.debezium.examples.caching.commons.OrderLineStatus;
import io.debezium.examples.caching.commons.PurchaseOrder;
import io.quarkus.infinispan.client.Remote;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * cache-update-service_1     | Received PO: {"id":1} - {"before":null,"after":{"id":1,"customerid":123,"orderdate":1548936781000000},"source":{"version":"1.4.0.Final","connector":"postgresql","name":"dbserver1","ts_ms":1610716846989,"snapshot":"false","db":"orderdb","schema":"inventory","table":"purchaseorder","txId":608,"lsn":34250896,"xmin":null},"op":"c","ts_ms":1610716847249,"transaction":null}
 * cache-update-service_1     | Received OL: {"id":1} - {"before":null,"after":{"id":1,"item":"Debezium in Action","quantity":2,"status":"ENTERED","totalprice":"D54=","order_id":1},"source":{"version":"1.4.0.Final","connector":"postgresql","name":"dbserver1","ts_ms":1610716846989,"snapshot":"false","db":"orderdb","schema":"inventory","table":"orderline","txId":608,"lsn":34251136,"xmin":null},"op":"c","ts_ms":1610716847259,"transaction":null}
 * cache-update-service_1     | Received OL: {"id":2} - {"before":null,"after":{"id":2,"item":"Debezium for Dummies","quantity":1,"status":"ENTERED","totalprice":"C7c=","order_id":1},"source":{"version":"1.4.0.Final","connector":"postgresql","name":"dbserver1","ts_ms":1610716846989,"snapshot":"false","db":"orderdb","schema":"inventory","table":"orderline","txId":608,"lsn":34251464,"xmin":null},"op":"c","ts_ms":1610716847264,"transaction":null}
 * cache-update-service_1     | Received OL: {"id":2} - {"before":null,"after":{"id":2,"item":"Debezium for Dummies","quantity":1,"status":"CANCELLED","totalprice":"C7c=","order_id":1},"source":{"version":"1.4.0.Final","connector":"postgresql","name":"dbserver1","ts_ms":1610716988917,"snapshot":"false","db":"orderdb","schema":"inventory","table":"orderline","txId":609,"lsn":34251752,"xmin":null},"op":"u","ts_ms":1610716989245,"transaction":null}
 * PO+OL {"id":1,"lines":[{"id":1,"item":"Debezium in Action","quantity":2,"status":"ENTERED","order_id":1,"total_price":39.98},{"id":2,"item":"Debezium for Dummies","quantity":1,"status":"ENTERED","order_id":1,"total_price":29.99}],"customer_id":123,"order_date":1548936781000000.000000000}
 */

@ApplicationScoped
public class KafkaEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaEventConsumer.class);

    @Inject
    @Remote("orders")
    RemoteCache<String, PurchaseOrder> orders;

    @Incoming("orders")
    public CompletionStage<Void> onPurchaseOrder(KafkaRecord<String, String> message) throws IOException {
        return CompletableFuture.runAsync(() -> {
            System.out.println("PO " + message.getPayload());
        }).thenRun(() -> message.ack());
    }

    @Incoming("orderlines")
    public CompletionStage<Void> onOrderLine(KafkaRecord<String, String> message) throws IOException {
        return CompletableFuture.runAsync(() -> {
            System.out.println("OL " + message.getPayload());
        }).thenRun(() -> message.ack());
    }

    @Incoming("orderwithlines")
    public CompletionStage<Void> onOrderWithLines(KafkaRecord<String, String> message) throws IOException {
        return CompletableFuture.runAsync(() -> {
            System.out.println("PO+OL " + message.getPayload());
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode;
            try {
                jsonNode = objectMapper.readTree(message.getPayload());
                Long orderId = jsonNode.get("id").asLong();
                LocalDateTime orderDate = Instant.ofEpochMilli(jsonNode.get("order_date").asLong()).atZone(ZoneId.systemDefault()).toLocalDateTime();
                Long customerId = jsonNode.get("customer_id").asLong();
                PurchaseOrder protoPurchaseOrder = new PurchaseOrder(orderId, customerId, orderDate,
                      new ArrayList<>());
                JsonNode lines = jsonNode.withArray("lines");
                Iterator<JsonNode> elements = lines.elements();
                while (elements.hasNext()) {
                    JsonNode orderLineNode = elements.next();
                    Long lineId = orderLineNode.get("id").asLong();
                    String item = orderLineNode.get("item").asText();
                    Integer quantity = orderLineNode.get("quantity").asInt();
                    BigDecimal price = orderLineNode.get("total_price").decimalValue();
                    OrderLineStatus status = OrderLineStatus.valueOf(orderLineNode.get("status").asText());
                    OrderLine orderLine = new OrderLine(lineId, item, quantity, price, status);
                    protoPurchaseOrder.getLineItems().add(orderLine);
                }
                orders.put(orderId.toString(), protoPurchaseOrder);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }).thenRun(() -> message.ack());
    }
}
