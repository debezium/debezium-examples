/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.testcontainers;

import static org.fest.assertions.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import com.jayway.jsonpath.JsonPath;

import io.debezium.testing.testcontainers.ConnectorConfiguration;
import io.debezium.testing.testcontainers.DebeziumContainer;

public class DebeziumContainerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebeziumContainerTest.class);

    private static Network network = Network.newNetwork();

    private static KafkaContainer kafkaContainer = new KafkaContainer()
            .withNetwork(network);

    public static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("debezium/postgres:11")
            .withNetwork(network)
            .withNetworkAliases("postgres");

    public static DebeziumContainer debeziumContainer = new DebeziumContainer(System.getProperty("debeziumVersion"))
            .withNetwork(network)
            .withKafka(kafkaContainer)
            .withLogConsumer(new Slf4jLogConsumer(LOGGER))
            .dependsOn(kafkaContainer);

    @BeforeClass
    public static void startContainers() {
        Startables.deepStart(Stream.of(
                kafkaContainer, postgresContainer, debeziumContainer)).join();
    }

    @Test
    public void shouldStreamChangeEventsFromPostgres() throws Exception {
        try (Connection connection = getConnection(postgresContainer);
                Statement statement = connection.createStatement();
                KafkaConsumer<String, String> consumer = getConsumer(kafkaContainer)) {

            statement.execute("create schema todo");
            statement.execute("create table todo.Todo (id int8 not null, title varchar(255), primary key (id))");
            statement.execute("alter table todo.Todo replica identity full");
            statement.execute("insert into todo.Todo values (1, 'Be Awesome')");
            statement.execute("insert into todo.Todo values (2, 'Learn Quarkus')");

            // host, database, user etc. are obtained from the container
            ConnectorConfiguration config = ConnectorConfiguration.forJdbcContainer(postgresContainer)
                    .with("database.server.name", "dbserver1");

            debeziumContainer.registerConnector("my-connector", config);

            consumer.subscribe(Arrays.asList("dbserver1.todo.todo"));

            List<ConsumerRecord<String, String>> changeEvents = drain(consumer, 2);

            assertThat(JsonPath.<Integer> read(changeEvents.get(0).key(), "$.id")).isEqualTo(1);
            assertThat(JsonPath.<String> read(changeEvents.get(0).value(), "$.op")).isEqualTo("r");
            assertThat(JsonPath.<String> read(changeEvents.get(0).value(), "$.after.title")).isEqualTo("Be Awesome");

            assertThat(JsonPath.<Integer> read(changeEvents.get(1).key(), "$.id")).isEqualTo(2);
            assertThat(JsonPath.<String> read(changeEvents.get(1).value(), "$.op")).isEqualTo("r");
            assertThat(JsonPath.<String> read(changeEvents.get(1).value(), "$.after.title")).isEqualTo("Learn Quarkus");

            statement.execute("update todo.Todo set title = 'Learn Java' where id = 2");

            changeEvents = drain(consumer, 1);

            assertThat(JsonPath.<Integer> read(changeEvents.get(0).key(), "$.id")).isEqualTo(2);
            assertThat(JsonPath.<String> read(changeEvents.get(0).value(), "$.op")).isEqualTo("u");
            assertThat(JsonPath.<String> read(changeEvents.get(0).value(), "$.before.title")).isEqualTo("Learn Quarkus");
            assertThat(JsonPath.<String> read(changeEvents.get(0).value(), "$.after.title")).isEqualTo("Learn Java");

            consumer.unsubscribe();
        }
    }

    private Connection getConnection(PostgreSQLContainer<?> postgresContainer) throws SQLException {
        return DriverManager.getConnection(postgresContainer.getJdbcUrl(), postgresContainer.getUsername(),
                postgresContainer.getPassword());
    }

    private KafkaConsumer<String, String> getConsumer(KafkaContainer kafkaContainer) {
        return new KafkaConsumer<>(
                ImmutableMap.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG, "tc-" + UUID.randomUUID(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(),
                new StringDeserializer());
    }

    private List<ConsumerRecord<String, String>> drain(KafkaConsumer<String, String> consumer, int expectedRecordCount) {
        List<ConsumerRecord<String, String>> allRecords = new ArrayList<>();

        Awaitility.await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_HUNDRED_MILLISECONDS).until(() -> {
            consumer.poll(java.time.Duration.ofMillis(50))
            .iterator()
            .forEachRemaining(allRecords::add);

            return allRecords.size() == expectedRecordCount;
        });

        return allRecords;
    }
}
