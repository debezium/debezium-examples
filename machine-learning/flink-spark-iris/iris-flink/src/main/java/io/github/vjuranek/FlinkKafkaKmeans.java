package io.github.vjuranek;

import java.util.Arrays;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.ml.clustering.kmeans.KMeansModelData;
import org.apache.flink.ml.clustering.kmeans.OnlineKMeans;
import org.apache.flink.ml.clustering.kmeans.OnlineKMeansModel;
import org.apache.flink.ml.linalg.DenseVector;
import org.apache.flink.ml.linalg.Vectors;
import org.apache.flink.ml.linalg.typeinfo.DenseVectorTypeInfo;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

public class FlinkKafkaKmeans {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        KafkaSource<ObjectNode> train = KafkaSource.<ObjectNode>builder()
                .setBootstrapServers("kafka:9092")
                .setTopics("flink.public.iris_train")
                .setClientIdPrefix("train")
                .setGroupId("dbz")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setDeserializer(KafkaRecordDeserializationSchema.of(new JSONKeyValueDeserializationSchema(false)))
                .build();
        DataStreamSource<ObjectNode> trainStream = env.fromSource(train, WatermarkStrategy.noWatermarks(), "Debezium train");

        KafkaSource<ObjectNode> test = KafkaSource.<ObjectNode>builder()
                .setBootstrapServers("kafka:9092")
                .setTopics("flink.public.iris_test")
                .setClientIdPrefix("test")
                .setGroupId("dbz")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setDeserializer(KafkaRecordDeserializationSchema.of(new JSONKeyValueDeserializationSchema(false)))
                .build();
        DataStreamSource<ObjectNode> testStream = env.fromSource(test, WatermarkStrategy.noWatermarks(), "Debezium test");

        KafkaSink<String> kafkaSink = KafkaSink.<String>builder()
                .setBootstrapServers("kafka:9092")
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("iris_predictions")
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build()
                )
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env);
        TypeInformation<?>[] types = {DenseVectorTypeInfo.INSTANCE};
        String names[] = {"features"};
        RowTypeInfo typeInfo = new RowTypeInfo(types, names);

        DataStream<Row> inputStream = trainStream.map(new RecordMapper()).returns(typeInfo);
        Table trainTable = tEnv.fromDataStream(inputStream).as("features");

        OnlineKMeans onlineKMeans = new OnlineKMeans()
                .setFeaturesCol("features")
                .setPredictionCol("prediction")
                //.setInitialModelData(KMeansModelData.generateRandomModelData(tEnv, 3, 4, 0.0, 0))
                .setInitialModelData(tEnv.fromDataStream(env.fromElements(1).map(new IrisInitCentroids())))
                .setK(3);
        OnlineKMeansModel model = onlineKMeans.fit(trainTable);

        DataStream<Row> testInputStream = testStream.map(new RecordMapper()).returns(typeInfo);
        Table testTable = tEnv.fromDataStream(testInputStream).as("features");
        Table outputTable = model.transform(testTable)[0];

        DataStream<Row> resultStream = tEnv.toChangelogStream(outputTable);
        resultStream.map(new ResultMapper()).sinkTo(kafkaSink);

        env.execute("KMeans Iris");
    }

private static class RecordMapper implements MapFunction<ObjectNode, Row> {
    @Override
    public Row map(ObjectNode node) {
        JsonNode payload = node.get("value").get("payload");
        StringBuffer sb = new StringBuffer();
        return Row.of(Vectors.dense(
                        payload.get("sepal_length").asDouble(),
                        payload.get("sepal_width").asDouble(),
                        payload.get("petal_length").asDouble(),
                        payload.get("petal_width").asDouble()));
    }
}

    private static class ResultMapper implements MapFunction<Row, String> {
        @Override
        public String map(Row result) {
            return String.format(String.format("%s is classified as %s", result.getField("features"), result.getField("prediction")));
        }
    }

    private static class IrisInitCentroids implements MapFunction<Integer, KMeansModelData> {
        @Override
        public KMeansModelData map(Integer integer) {
            DenseVector[] centroids = new DenseVector[3];
            centroids[0] = new DenseVector(new double[] {6.0, 3.0, 1.0, 0.0});
            centroids[1] = new DenseVector(new double[] {6.0, 3.0, 2.0, 1.0});
            centroids[2] = new DenseVector(new double[] {6.0, 3.0, 3.0, 2.0});

            DenseVector weights = new DenseVector(3);
            Arrays.fill(weights.values, 0);
            return new KMeansModelData(centroids, weights);
        }
    }
}
