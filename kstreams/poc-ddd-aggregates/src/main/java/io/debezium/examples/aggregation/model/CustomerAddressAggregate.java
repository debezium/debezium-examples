package io.debezium.examples.aggregation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CustomerAddressAggregate {

    private final Customer customer;

    private final List<Address> addresses;

    @JsonCreator
    public CustomerAddressAggregate(
            @JsonProperty("customer") Customer customer,
            @JsonProperty("addresses") List<Address> addresses) {
        this.customer = customer;
        this.addresses = addresses;
    }

    public Customer getCustomer() {
        return customer;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    @Override
    public String toString() {
        return "CustomerAddressAggregate{" +
                "customer=" + customer +
                ", addresses=" + addresses +
                '}';
    }

}
