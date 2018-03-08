package io.debezium.examples.aggregation;

import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Serialized;
import org.apache.kafka.streams.state.KeyValueStore;

import io.debezium.examples.aggregation.model.Address;
import io.debezium.examples.aggregation.model.Addresses;
import io.debezium.examples.aggregation.model.Customer;
import io.debezium.examples.aggregation.model.CustomerAddressAggregate;
import io.debezium.examples.aggregation.model.DefaultId;
import io.debezium.examples.aggregation.model.EventType;
import io.debezium.examples.aggregation.model.LatestAddress;
import io.debezium.examples.aggregation.serdes.SerdeFactory;

public class StreamingAggregatesDDD {

    public static void main(String[] args) {

        if(args.length != 3) {
            System.err.println("usage: java -jar <package> "
                    + StreamingAggregatesDDD.class.getName() + " <parent_topic> <children_topic> <bootstrap_servers>");
            System.exit(-1);
        }

        final String parentTopic = args[0];
        final String childrenTopic = args[1];
        final String bootstrapServers = args[2];

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streaming-aggregates-ddd");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10*1024);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(CommonClientConfigs.METADATA_MAX_AGE_CONFIG, 500);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final Serde<DefaultId> defaultIdSerde = SerdeFactory.createDbzEventJsonPojoSerdeFor(DefaultId.class,true);
        final Serde<Customer> customerSerde = SerdeFactory.createDbzEventJsonPojoSerdeFor(Customer.class,false);
        final Serde<Address> addressSerde = SerdeFactory.createDbzEventJsonPojoSerdeFor(Address.class,false);
        final Serde<LatestAddress> latestAddressSerde = SerdeFactory.createDbzEventJsonPojoSerdeFor(LatestAddress.class,false);
        final Serde<Addresses> addressesSerde = SerdeFactory.createDbzEventJsonPojoSerdeFor(Addresses.class,false);
        final Serde<CustomerAddressAggregate> aggregateSerde =
                SerdeFactory.createDbzEventJsonPojoSerdeFor(CustomerAddressAggregate.class,false);

        StreamsBuilder builder = new StreamsBuilder();

        //1) read parent topic i.e. customers as ktable
        KTable<DefaultId, Customer> customerTable =
                builder.table(parentTopic, Consumed.with(defaultIdSerde,customerSerde));

        //2) read children topic i.e. addresses as kstream
        KStream<DefaultId, Address> addressStream = builder.stream(childrenTopic,
                Consumed.with(defaultIdSerde, addressSerde));

        //2a) pseudo-aggreate addresses to keep latest relationship info
        KTable<DefaultId,LatestAddress> tempTable = addressStream
                .groupByKey(Serialized.with(defaultIdSerde, addressSerde))
                .aggregate(
                        () -> new LatestAddress(),
                        (DefaultId addressId, Address address, LatestAddress latest) -> {
                            latest.update(address,addressId,new DefaultId(address.getCustomer_id()));
                            return latest;
                        },
                        Materialized.<DefaultId,LatestAddress,KeyValueStore<Bytes, byte[]>>
                                        as(childrenTopic+"_table_temp")
                                            .withKeySerde(defaultIdSerde)
                                                .withValueSerde(latestAddressSerde)
                );

        //2b) aggregate addresses per customer id
        KTable<DefaultId, Addresses> addressTable = tempTable.toStream()
                .map((addressId, latestAddress) -> new KeyValue<>(latestAddress.getCustomerId(),latestAddress))
                .groupByKey(Serialized.with(defaultIdSerde,latestAddressSerde))
                .aggregate(
                        () -> new Addresses(),
                        (customerId, latestAddress, addresses) -> {
                            addresses.update(latestAddress);
                            return addresses;
                        },
                        Materialized.<DefaultId,Addresses,KeyValueStore<Bytes, byte[]>>
                                        as(childrenTopic+"_table_aggregate")
                                            .withKeySerde(defaultIdSerde)
                                                .withValueSerde(addressesSerde)
                );

        //3) KTable-KTable JOIN to combine customer and addresses
        KTable<DefaultId,CustomerAddressAggregate> dddAggregate =
                customerTable.join(addressTable, (customer, addresses) ->
                    customer.get_eventType() == EventType.DELETE ?
                            null : new CustomerAddressAggregate(customer,addresses.getEntries())
                );

        dddAggregate.toStream().to("final_ddd_aggregates",
                                    Produced.with(defaultIdSerde,(Serde)aggregateSerde));

        dddAggregate.toStream().print(Printed.toSysOut());

        final KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
