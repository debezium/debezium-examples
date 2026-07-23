package io.debezium.examples.flink.join;

import static org.apache.flink.table.api.Expressions.$;

import java.io.Serializable;
import java.util.Properties;
import java.util.stream.StreamSupport;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.api.java.typeutils.TupleTypeInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.formats.json.JsonRowSerializationSchema;
import org.apache.flink.kafka.shaded.org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer.Semantic;
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

import io.debezium.examples.flink.join.model.Address;

public class FlinkJoinTableStream {

    public interface Mapper<T> extends Serializable {

        T map(ObjectMapper mapper, JsonNode object);

    }

    /**
     * Specialized unwrap of Debezium payloads that conveys the key, whether it's an upsert or delete, and the full record
     * this is similar to the handling with flink debezium tables, but does not create both a before and after update
     */
    public static class DebeziumUnwrap<K, V> extends RichMapFunction<ObjectNode, Tuple3<K, Boolean, V>> {

        ObjectMapper objectMapper = new ObjectMapper();
        Mapper<K> keyMapper;
        Mapper<V> valueMapper;

        public DebeziumUnwrap(Mapper<K> keyMapper, Mapper<V> valueMapper) {
            this.keyMapper = keyMapper;
            this.valueMapper = valueMapper;
        }

        @Override
        public Tuple3<K, Boolean, V> map(ObjectNode r) throws Exception {
            JsonNode jsonNode = r.get("value");
            String op = jsonNode.get("op").asText();
            JsonNode value = jsonNode.get("after");
            boolean upsert = true;
            if ("d".equals(op)) {
                value = jsonNode.get("before");
                upsert = false;
            }
            return Tuple3.of(keyMapper.map(objectMapper, r.get("key")), upsert, valueMapper.map(objectMapper, value));
        }
    }

    /**
     * Specialized aggregate that collects rows by key "id", and outputs by "customer_id"
     * The input matches the output of the DebeziumUnwrap above, but with the expectation that the stream is partitioned by "customer_id"
     */
    public static class AddressAgg extends RichMapFunction<Tuple3<Long, Boolean, Address>, Tuple2<Integer, Address[]>> {

        private transient MapState<Long, Address> map;

        @Override
        public Tuple2<Integer, Address[]> map(Tuple3<Long, Boolean, Address> value) throws Exception {
            if (value.f1) {
                map.put(value.f2.id, value.f2);
            } else {
                map.remove(value.f2.id);
            }

            return Tuple2.of(value.f2.customer_id,
                    StreamSupport.stream(map.values().spliterator(), false).toArray(Address[]::new));
        }

        @Override
        public void open(Configuration config) {
            map = getRuntimeContext().getMapState(new MapStateDescriptor<>("values", Long.class, Address.class));
        }
    }

    public static String TOPIC_OUT = "customers-with-addresses";

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        //add customers as a table
        tableEnv.executeSql("CREATE TABLE customers (\n" +
                "  id INT PRIMARY KEY,\n" +
                "  first_name STRING,\n" +
                "  last_name STRING,\n" +
                "  email STRING\n" +
                ") WITH (\n" +
                " 'connector' = 'kafka',\n" +
                " 'topic' = 'dbserver1.inventory.customers',\n" +
                " 'properties.bootstrap.servers' = 'localhost:9092',\n" +
                " 'properties.group.id' = '1',\n" +
                " 'format' = 'debezium-json',\n" +
                " 'scan.startup.mode' = 'earliest-offset'\n" +
                ")");

        //add addresses as a stream
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9092");
        properties.setProperty("group.id", "1");

        FlinkKafkaConsumer<ObjectNode> function = new FlinkKafkaConsumer<>("dbserver1.inventory.addresses",
                new JSONKeyValueDeserializationSchema(false), properties);
        function.setStartFromEarliest();
        DataStream<ObjectNode> addressStream = env.addSource(function);

        //consume the generic json stream with specialized debezium handling
        //to build what is effectively an append stream of address aggregates
        DataStream<Tuple2<Integer, Address[]>> agg = addressStream
            .filter((r)->r.has("value")) //skip tombstones
            .map(new DebeziumUnwrap<Long, Address>((mapper, n) -> n.asLong(),
                        (mapper, n) -> mapper.convertValue(n, Address.class))
                    , Types.TUPLE(Types.LONG, Types.BOOLEAN, TypeInformation.of(Address.class)))
            .keyBy((t)->t.f2.customer_id)
            .map(new AddressAgg());

        //join customers to table created from the aggregation.  we're using the table api without idle state expiration
        //to process the join indefinitely without a window
        Table join = tableEnv.from("customers")
            .leftOuterJoin(tableEnv.fromDataStream(agg, $("customer_id"), $("addresses")), $("id").isEqual($("customer_id")))
            .dropColumns($("customer_id")); //drop the id from the right side

        //we cannot directly write this table result, which has a retract stream as input
        //into an output kafka table, so we create an output stream to direct into the output topic

        //we don't have to filter before update events as they don't make it through this join
        //but that isn't great because that seems to give us ephemeral deletes on some modifications
        DataStream<Tuple2<Boolean,Row>> output = tableEnv.toRetractStream(join, Row.class);

        //we have to use the appropriate type information for serialization
        //under the covers flink has substituted Row instances for the pojos
        //the stream type reflects that
        TypeInformation<Row> rowType = ((TupleTypeInfo)output.getType()).getTypeAt(1);
        JsonRowSerializationSchema rowSerialization = JsonRowSerializationSchema.builder().withTypeInfo(rowType).build();
        //output the key as the first field
        JsonRowSerializationSchema keySerialization = JsonRowSerializationSchema.builder()
                .withTypeInfo(new RowTypeInfo(Types.INT)).build();

        //only output the value on upsert events, null on delete
        FlinkKafkaProducer<Tuple2<Boolean, Row>> kafkaProducer = new FlinkKafkaProducer<Tuple2<Boolean, Row>>(TOPIC_OUT,
                ((record, timestamp) -> new ProducerRecord<byte[], byte[]>(TOPIC_OUT,
                        keySerialization.serialize(record.f1),
                        record.f0 ? rowSerialization.serialize(record.f1) : null)),
                properties,
                Semantic.EXACTLY_ONCE);

        output.addSink(kafkaProducer);

        env.execute("Debezium Join");
    }

}
