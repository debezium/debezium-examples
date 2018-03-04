package io.debezium.examples.aggregation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LatestAddress {

    private DefaultId addressId;

    private DefaultId customerId;

    private Address latest;

    public LatestAddress() {}

    @JsonCreator
    public LatestAddress(
            @JsonProperty("addressId") DefaultId addressId,
            @JsonProperty("customerId") DefaultId customerId,
            @JsonProperty("latest") Address latest) {
        this.addressId = addressId;
        this.customerId = customerId;
        this.latest = latest;
    }

    public void update(Address address, DefaultId addressId, DefaultId customerId) {
        if(EventType.DELETE == address.get_eventType()) {
            latest = null;
            return;
        }
        latest = address;
        this.addressId = addressId;
        this.customerId = customerId;
    }

    public DefaultId getAddressId() {
        return addressId;
    }

    public DefaultId getCustomerId() {
        return customerId;
    }

    public Address getLatest() {
        return latest;
    }

    @Override
    public String toString() {
        return "LatestChild{" +
            "addressId=" + addressId +
            ", customerId=" + customerId +
            ", latest=" + latest +
            '}';
    }
}
