package io.debezium.examples.kstreams.fkjoin.streams;

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

import io.debezium.examples.kstreams.fkjoin.model.Address;
import io.debezium.examples.kstreams.fkjoin.model.AddressAndCustomer;
import io.debezium.examples.kstreams.fkjoin.model.Customer;
import io.debezium.examples.kstreams.fkjoin.model.CustomerWithAddresses;
import io.debezium.serde.DebeziumSerdes;
import io.quarkus.kafka.client.serialization.JsonbSerde;

@ApplicationScoped
public class TopologyProducer {

    @ConfigProperty(name = "customers.topic")
    String customersTopic;

    @ConfigProperty(name = "addresses.topic")
    String addressesTopic;

    @ConfigProperty(name = "customers.with.addresses.topic")
    String customersWithAddressesTopic;

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        Serde<Long> adressKeySerde = DebeziumSerdes.payloadJson(Long.class);
        adressKeySerde.configure(Collections.emptyMap(), true);
        Serde<Address> addressSerde = DebeziumSerdes.payloadJson(Address.class);
        addressSerde.configure(Collections.singletonMap("from.field", "after"), false);
        
        Serde<Integer> customersKeySerde = DebeziumSerdes.payloadJson(Integer.class);
        customersKeySerde.configure(Collections.emptyMap(), true);
        Serde<Customer> customersSerde = DebeziumSerdes.payloadJson(Customer.class);
        customersSerde.configure(Collections.singletonMap("from.field", "after"), false);
        
        JsonbSerde<AddressAndCustomer> addressAndCustomerSerde = new JsonbSerde<>(AddressAndCustomer.class);
        JsonbSerde<CustomerWithAddresses> customerWithAddressesSerde = new JsonbSerde<>(CustomerWithAddresses.class);
        
        KTable<Long, Address> addresses = builder.table(
                addressesTopic,
                Consumed.with(adressKeySerde, addressSerde)
        );
        
        KTable<Integer, Customer> customers = builder.table(
                customersTopic,
                Consumed.with(customersKeySerde, customersSerde)
        );
        
        KTable<Integer, CustomerWithAddresses> customersWithAddresses = addresses.join(
                customers,
                address -> address.customer_id,
                (address, customer) -> new AddressAndCustomer(address, customer),
                Materialized.with(Serdes.Long(), addressAndCustomerSerde))
            .groupBy(
                (Long addressId, AddressAndCustomer value) -> KeyValue.pair(value.customer.id, value),
                Grouped.with(Serdes.Integer(), addressAndCustomerSerde))
            .aggregate(
                CustomerWithAddresses::new,
                (Integer key, AddressAndCustomer value, CustomerWithAddresses aggregate) -> {
                        aggregate.addAddress(value);
                        return aggregate;
                },
                (Integer key, AddressAndCustomer value, CustomerWithAddresses aggregate) -> {
                        aggregate.removeAddress(value);
                        return aggregate;
                },
                Materialized.with(Serdes.Integer(), customerWithAddressesSerde));
        
        customersWithAddresses.toStream()
        .to(
                customersWithAddressesTopic,
                Produced.with(Serdes.Integer(), customerWithAddressesSerde)
        );
        
        return builder.build();
    }
}
