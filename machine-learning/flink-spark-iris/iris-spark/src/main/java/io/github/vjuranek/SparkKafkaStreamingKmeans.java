package io.github.vjuranek;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.mllib.clustering.StreamingKMeans;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import scala.Tuple2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SparkKafkaStreamingKmeans {
    public static void main(String[] args) throws Exception {
        SparkConf conf = new SparkConf().setMaster("local[2]").setAppName("iris-kmeans");
        JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(10));


        Set<String> trainTopic = new HashSet<>(Arrays.asList("spark.public.iris_train"));
        Set<String> testTopic = new HashSet<>(Arrays.asList("spark.public.iris_test"));
        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, "dbz");
        kafkaParams.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        JavaInputDStream<ConsumerRecord<String, String>> trainStream = KafkaUtils.createDirectStream(
                jssc,
                LocationStrategies.PreferConsistent(),
                ConsumerStrategies.Subscribe(trainTopic, kafkaParams));
        JavaDStream<LabeledPoint> train = trainStream.map(ConsumerRecord::value)
                .map(SparkKafkaStreamingKmeans::toLabeledPointString)
                .map(LabeledPoint::parse)
                .cache();
        JavaInputDStream<ConsumerRecord<String, String>> testStream = KafkaUtils.createDirectStream(
                jssc,
                LocationStrategies.PreferConsistent(),
                ConsumerStrategies.Subscribe(testTopic, kafkaParams));
        JavaDStream<LabeledPoint> test = testStream.map(ConsumerRecord::value)
                .map(SparkKafkaStreamingKmeans::toLabeledPointString)
                .map(LabeledPoint::parse);

        Vector[] initCenters = new Vector[]{
                Vectors.dense(new double[]{6.0, 3.0, 1.0, 0.0}),
                Vectors.dense(new double[]{6.0, 3.0, 2.0, 1.0}),
                Vectors.dense(new double[]{6.0, 3.0, 3.0, 2.0})
        };
        double[] weights = new double[]{0.0, 0.0, 0.0};

        StreamingKMeans model = new StreamingKMeans()
                .setK(3)
                .setInitialCenters(initCenters, weights);

        model.trainOn(train.map(lp -> lp.getFeatures()));

        JavaPairDStream<Double, Vector> predict = test.mapToPair(lp -> new Tuple2<>(lp.label(), lp.features()));
        model.predictOnValues(predict).print(11);

        jssc.start();
        jssc.awaitTermination();
        jssc.stop();
    }

    private static String toLabeledPointString(String json) throws ParseException {
        JSONParser jsonParser = new JSONParser();
        JSONObject o = (JSONObject)jsonParser.parse(json);
        return String.format("%s, %s %s %s %s",
                o.get("iris_class"),
                o.get("sepal_length"),
                o.get("sepal_width"),
                o.get("petal_length"),
                o.get("petal_width"));
    }
}
