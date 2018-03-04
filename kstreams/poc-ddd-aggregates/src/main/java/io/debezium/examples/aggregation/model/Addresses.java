package io.debezium.examples.aggregation.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Addresses {

    @JsonProperty
    @JsonSerialize(keyUsing = DefaultId.IdSerializer.class)
    @JsonDeserialize(keyUsing = DefaultId.IdDeserializer.class)
    private Map<DefaultId,Address> entries = new LinkedHashMap<>();

    public void update(LatestAddress address) {
        if(address.getLatest() != null) {
            entries.put(address.getAddressId(),address.getLatest());
        } else {
            entries.remove(address.getAddressId());
        }
    }

    @JsonIgnore
    public List<Address> getEntries() {
        return new ArrayList<>(entries.values());
    }

    @Override
    public String toString() {
        return "Addresses{" +
            "entries=" + entries +
            '}';
    }
}
