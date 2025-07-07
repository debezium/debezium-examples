package io.debezium.examples.openlineage;


import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.ProcessJoinFunction;


import org.apache.flink.util.Collector;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Duration;

public class OrderShippingProcessor {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Configure Kafka sources for Debezium topics
        KafkaSource<String> ordersSource = KafkaSource.<String>builder()
            .setBootstrapServers("kafka:9092")
            .setTopics("inventory.inventory.orders")
            .setGroupId("flink-order-processor")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new SimpleStringSchema())
            .build();

        KafkaSource<String> customersSource = KafkaSource.<String>builder()
            .setBootstrapServers("kafka:9092")
            .setTopics("inventory.inventory.customers")
            .setGroupId("flink-order-processor")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new SimpleStringSchema())
            .build();

        KafkaSource<String> productsSource = KafkaSource.<String>builder()
            .setBootstrapServers("kafka:9092")
            .setTopics("inventory.inventory.products")
            .setGroupId("flink-order-processor")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new SimpleStringSchema())
            .build();

        // Create data streams
        DataStream<String> ordersStream = env.fromSource(ordersSource,
            WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(20))
                .withTimestampAssigner((event, timestamp) -> extractTimestamp(event)),
            "Orders Source");

        DataStream<String> customersStream = env.fromSource(customersSource,
            WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(20))
                .withTimestampAssigner((event, timestamp) -> extractTimestamp(event)),
            "Customers Source");

        DataStream<String> productsStream = env.fromSource(productsSource,
            WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(20))
                .withTimestampAssigner((event, timestamp) -> extractTimestamp(event)),
            "Products Source");

        // Transform streams to extract relevant data
        DataStream<OrderEvent> orders = ordersStream
            .filter(record -> isInsertUpdateReadEvent(record))
            .map(new OrderEventMapper())
            .keyBy(OrderEvent::getOrderId);

        DataStream<CustomerEvent> customers = customersStream
            .filter(record -> isInsertUpdateReadEvent(record))
            .map(new CustomerEventMapper())
            .keyBy(CustomerEvent::getCustomerId);

        DataStream<ProductEvent> products = productsStream
            .filter(record -> isInsertUpdateReadEvent(record))
            .map(new ProductEventMapper())
            .keyBy(ProductEvent::getProductId);

        // Join orders with customers
        DataStream<OrderCustomerJoined> orderCustomerJoined = orders
            .keyBy(OrderEvent::getPurchaserId)
            .intervalJoin(customers.keyBy(CustomerEvent::getCustomerId))
            .between(Duration.ofMinutes(-5), Duration.ofMinutes(5))
            .process(new OrderCustomerJoinFunction());

        // Join the result with products
        DataStream<ShippingOrder> shippingOrders = orderCustomerJoined
            .keyBy(OrderCustomerJoined::getProductId)
            .intervalJoin(products.keyBy(ProductEvent::getProductId))
            .between(Duration.ofMinutes(-5), Duration.ofMinutes(5))
            .process(new OrderProductJoinFunction());

        // Convert to JSON and send to output topic
        DataStream<String> shippingOrdersJson = shippingOrders
            .map(new ShippingOrderToJsonMapper());

        // Configure Kafka sink
        KafkaSink<String> sink = KafkaSink.<String>builder()
            .setBootstrapServers("kafka:9092")
            .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                .setTopic("shipping-orders")
                .setValueSerializationSchema(new SimpleStringSchema())
                .build())
            .build();

        // Send to output topic
        shippingOrdersJson.sinkTo(sink);

        // Execute the job
        env.execute("Order Shipping Processor");
    }

    // Helper methods
    private static boolean isInsertUpdateReadEvent(String record) {
        try {
            JsonNode jsonNode = objectMapper.readTree(record);
            JsonNode payload = jsonNode.get("payload");
            if (payload != null) {
                JsonNode op = payload.get("op");
                return op != null && (op.asText().equals("c") || op.asText().equals("u") || op.asText().equals("r"));
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static long extractTimestamp(String record) {
        try {
            JsonNode jsonNode = objectMapper.readTree(record);
            JsonNode payload = jsonNode.get("payload");
            if (payload != null) {
                JsonNode ts = payload.get("ts_ms");
                if (ts != null) {
                    return ts.asLong();
                }
            }
            return System.currentTimeMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    // Data classes
    public static class OrderEvent {
        public int orderId;
        public String orderDate;
        public int purchaserId;
        public int quantity;
        public int productId;

        // Constructors, getters, setters
        public OrderEvent() {}

        public OrderEvent(int orderId, String orderDate, int purchaserId, int quantity, int productId) {
            this.orderId = orderId;
            this.orderDate = orderDate;
            this.purchaserId = purchaserId;
            this.quantity = quantity;
            this.productId = productId;
        }

        public int getOrderId() { return orderId; }
        public String getOrderDate() { return orderDate; }
        public int getPurchaserId() { return purchaserId; }
        public int getQuantity() { return quantity; }
        public int getProductId() { return productId; }
    }

    public static class CustomerEvent {
        public int customerId;
        public String firstName;
        public String lastName;
        public String email;

        public CustomerEvent() {}

        public CustomerEvent(int customerId, String firstName, String lastName, String email) {
            this.customerId = customerId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }

        public int getCustomerId() { return customerId; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getEmail() { return email; }
    }

    public static class ProductEvent {
        public int productId;
        public String name;
        public String description;
        public double weight;

        public ProductEvent() {}

        public ProductEvent(int productId, String name, String description, double weight) {
            this.productId = productId;
            this.name = name;
            this.description = description;
            this.weight = weight;
        }

        public int getProductId() { return productId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public double getWeight() { return weight; }
    }

    public static class OrderCustomerJoined {
        public int orderId;
        public String orderDate;
        public int quantity;
        public int productId;
        public String customerFirstName;
        public String customerLastName;
        public String customerEmail;

        public int getOrderId() { return orderId; }
        public int getProductId() { return productId; }
        public int getQuantity() { return quantity; }
        public String getOrderDate() { return orderDate; }
        public String getCustomerFirstName() { return customerFirstName; }
        public String getCustomerLastName() { return customerLastName; }
        public String getCustomerEmail() { return customerEmail; }
    }

    public static class ShippingOrder {
        public int orderId;
        public String orderDate;
        public int quantity;
        public String productName;
        public String productDescription;
        public double productWeight;
        public double totalWeight;
        public String customerName;
        public String customerEmail;
        public String shippingStatus;

        public ShippingOrder() {}

        public ShippingOrder(int orderId, String orderDate, int quantity, String productName,
                           String productDescription, double productWeight, String customerName,
                           String customerEmail) {
            this.orderId = orderId;
            this.orderDate = orderDate;
            this.quantity = quantity;
            this.productName = productName;
            this.productDescription = productDescription;
            this.productWeight = productWeight;
            this.totalWeight = productWeight * quantity;
            this.customerName = customerName;
            this.customerEmail = customerEmail;
            this.shippingStatus = "READY_TO_SHIP";
        }
    }

    // Mapper functions
    public static class OrderEventMapper implements MapFunction<String, OrderEvent> {
        @Override
        public OrderEvent map(String value) throws Exception {
            JsonNode jsonNode = objectMapper.readTree(value);
            JsonNode after = jsonNode.get("payload").get("after");

            return new OrderEvent(
                after.get("id").asInt(),
                after.get("order_date").asText(),
                after.get("purchaser").asInt(),
                after.get("quantity").asInt(),
                after.get("product_id").asInt()
            );
        }
    }

    public static class CustomerEventMapper implements MapFunction<String, CustomerEvent> {
        @Override
        public CustomerEvent map(String value) throws Exception {
            JsonNode jsonNode = objectMapper.readTree(value);
            JsonNode after = jsonNode.get("payload").get("after");

            return new CustomerEvent(
                after.get("id").asInt(),
                after.get("first_name").asText(),
                after.get("last_name").asText(),
                after.get("email").asText()
            );
        }
    }

    public static class ProductEventMapper implements MapFunction<String, ProductEvent> {
        @Override
        public ProductEvent map(String value) throws Exception {
            JsonNode jsonNode = objectMapper.readTree(value);
            JsonNode after = jsonNode.get("payload").get("after");

            return new ProductEvent(
                after.get("id").asInt(),
                after.get("name").asText(),
                after.get("description").asText(),
                after.get("weight").asDouble()
            );
        }
    }

    // Join functions
    public static class OrderCustomerJoinFunction
        extends ProcessJoinFunction<OrderEvent, CustomerEvent, OrderCustomerJoined> {

        @Override
        public void processElement(OrderEvent order, CustomerEvent customer,
                                 Context context, Collector<OrderCustomerJoined> out) {
            OrderCustomerJoined joined = new OrderCustomerJoined();
            joined.orderId = order.orderId;
            joined.orderDate = order.orderDate;
            joined.quantity = order.quantity;
            joined.productId = order.productId;
            joined.customerFirstName = customer.firstName;
            joined.customerLastName = customer.lastName;
            joined.customerEmail = customer.email;

            out.collect(joined);
        }
    }

    public static class OrderProductJoinFunction
        extends ProcessJoinFunction<OrderCustomerJoined, ProductEvent, ShippingOrder> {

        @Override
        public void processElement(OrderCustomerJoined orderCustomer, ProductEvent product,
                                 Context context, Collector<ShippingOrder> out) {
            String customerName = orderCustomer.customerFirstName + " " + orderCustomer.customerLastName;

            ShippingOrder shippingOrder = new ShippingOrder(
                orderCustomer.orderId,
                orderCustomer.orderDate,
                orderCustomer.quantity,
                product.name,
                product.description,
                product.weight,
                customerName,
                orderCustomer.customerEmail
            );

            out.collect(shippingOrder);
        }
    }

    public static class ShippingOrderToJsonMapper implements MapFunction<ShippingOrder, String> {
        @Override
        public String map(ShippingOrder order) throws Exception {
            ObjectNode json = objectMapper.createObjectNode();
            json.put("orderId", order.orderId);
            json.put("orderDate", order.orderDate);
            json.put("quantity", order.quantity);
            json.put("productName", order.productName);
            json.put("productDescription", order.productDescription);
            json.put("productWeight", order.productWeight);
            json.put("totalWeight", order.totalWeight);
            json.put("customerName", order.customerName);
            json.put("customerEmail", order.customerEmail);
            json.put("shippingStatus", order.shippingStatus);
            json.put("processedAt", System.currentTimeMillis());

            return objectMapper.writeValueAsString(json);
        }
    }
}