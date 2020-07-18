package io.debezium.examples.kstreams.fkjoin.model;

public class AddressAndCustomer {

    public Address address;
    public Customer customer;

    public AddressAndCustomer() {
    }

    public AddressAndCustomer(Address address, Customer customer) {
        this.address = address;
        this.customer = customer;
    }

    public static AddressAndCustomer create(Address address, Customer customer) {
        return new AddressAndCustomer(address, customer);
    }

    public Address address() {
        return address;
    }

    @Override
    public String toString() {
        return "AddressAndCustomer [address=" + address + ", customer=" + customer + "]";
    }
}
