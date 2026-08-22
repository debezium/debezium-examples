package io.debezium.examples.flink.join;

import java.util.Properties;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.api.java.typeutils.TupleTypeInfo;
import org.apache.flink.formats.json.JsonRowSerializationSchema;
import org.apache.flink.kafka.shaded.org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer.Semantic;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.types.RowKind;

/**
 * Performs an inner join of a customer and an address
 */
public class FlinkJoin {

    public static String TOPIC_OUT = "customer-with-address";

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

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

        tableEnv.executeSql("CREATE TABLE addresses (\n" +
                "  id BIGINT PRIMARY KEY,\n" +
                "  customer_id INT,\n" +
                "  street STRING,\n" +
                "  city STRING,\n" +
                "  zipcode STRING,\n" +
                "  country STRING\n" +
                ") WITH (\n" +
                " 'connector' = 'kafka',\n" +
                " 'topic' = 'dbserver1.inventory.addresses',\n" +
                " 'properties.bootstrap.servers' = 'localhost:9092',\n" +
                " 'properties.group.id' = '1',\n" +
                " 'format' = 'debezium-json',\n" +
                " 'scan.startup.mode' = 'earliest-offset'\n" +
                ")");

        Table addressWithEmail = tableEnv.sqlQuery("select c.id, c.email, a.country, a.id as address_id "
                + "from customers as c inner join addresses as a on c.id = a.customer_id");

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9092");

        //-- coming soon in flink, we should be able to output a changelog/cdc stream
        //DebeziumJsonSerializationSchema schema ...

        //we cannot directly write this table result, which has a retract stream as input
        //into an output kafka table, so we create an output stream to direct into the output topic
        //we have to filter the before update as we want an unwrapped result
        DataStream<Tuple2<Boolean, Row>> output = tableEnv.toRetractStream(addressWithEmail, Row.class)
                .filter((t)->t.f1.getKind()!=RowKind.UPDATE_BEFORE);

        TypeInformation<Row> rowType = ((TupleTypeInfo)output.getType()).getTypeAt(1);
        JsonRowSerializationSchema rowSerialization = JsonRowSerializationSchema.builder().withTypeInfo(rowType).build();
        //output the key as the first field
        JsonRowSerializationSchema keySerialization = JsonRowSerializationSchema.builder()
                .withTypeInfo(new RowTypeInfo(Types.INT)).build();

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
