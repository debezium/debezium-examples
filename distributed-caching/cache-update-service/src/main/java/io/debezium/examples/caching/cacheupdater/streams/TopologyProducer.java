package io.debezium.examples.caching.cacheupdater.streams;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.debezium.examples.caching.cacheupdater.streams.model.OrderLine;
import io.debezium.examples.caching.cacheupdater.streams.model.OrderLineAndPurchaseOrder;
import io.debezium.examples.caching.cacheupdater.streams.model.OrderWithLines;
import io.debezium.examples.caching.cacheupdater.streams.model.PurchaseOrder;
import io.debezium.serde.DebeziumSerdes;
import io.quarkus.kafka.client.serialization.ObjectMapperSerde;

@ApplicationScoped
public class TopologyProducer {

    @ConfigProperty(name = "orders.topic")
    String ordersTopic;

    @ConfigProperty(name = "order.lines.topic")
    String orderLinesTopic;

    @ConfigProperty(name = "orders.with.lines.topic")
    String ordersWithLinesTopic;

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        Serde<Long> orderLineKeySerde = DebeziumSerdes.payloadJson(Long.class);
        orderLineKeySerde.configure(Collections.emptyMap(), true);
        Serde<OrderLine> orderLineSerde = DebeziumSerdes.payloadJson(OrderLine.class);
        orderLineSerde.configure(Collections.singletonMap("from.field", "after"), false);

        Serde<Long> ordersKeySerde = DebeziumSerdes.payloadJson(Long.class);
        ordersKeySerde.configure(Collections.emptyMap(), true);
        Serde<PurchaseOrder> ordersSerde = DebeziumSerdes.payloadJson(PurchaseOrder.class);
        ordersSerde.configure(Collections.singletonMap("from.field", "after"), false);

        ObjectMapperSerde<OrderLineAndPurchaseOrder> orderLineAndPurchaseOrderSerde = new ObjectMapperSerde<>(OrderLineAndPurchaseOrder.class);
        ObjectMapperSerde<OrderWithLines> orderWithLinesSerde = new ObjectMapperSerde<>(OrderWithLines.class);

        KTable<Long, OrderLine> orderLines = builder.table(
                orderLinesTopic,
                Consumed.with(orderLineKeySerde, orderLineSerde)
        );

        KTable<Long, PurchaseOrder> orders = builder.table(
                ordersTopic,
                Consumed.with(ordersKeySerde, ordersSerde)
        );

        KTable<Long, OrderWithLines> ordersWithLines = orderLines.join(
                orders,
                orderLine -> orderLine.order_id,
                OrderLineAndPurchaseOrder::new,
                Materialized.with(Serdes.Long(), orderLineAndPurchaseOrderSerde)
            )
            .groupBy(
                (orderLineId, orderLineAndOrder) -> KeyValue.pair(orderLineAndOrder.purchaseOrder.id, orderLineAndOrder),
                Grouped.with(Serdes.Long(), orderLineAndPurchaseOrderSerde)
            )
            .aggregate(
                OrderWithLines::new,
                (customerId, orderLineAndOrder, aggregate) -> aggregate.addOrderLine(orderLineAndOrder),
                (customerId, orderLineAndOrder, aggregate) -> aggregate.removeOrderLine(orderLineAndOrder),
                Materialized.with(Serdes.Long(), orderWithLinesSerde)
            );

        ordersWithLines.toStream()
        .to(
                ordersWithLinesTopic,
                Produced.with(Serdes.Long(), orderWithLinesSerde)
        );

        return builder.build();
    }
}
