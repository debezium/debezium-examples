package io.debezium.demos.auditing.admin;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VegetableEvent {

    private VegetableData before;
    private VegetableData after;
    private SourceData source;
    @JsonProperty("op")
    private String operation;
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    @JsonProperty("ts_ms")
    private Date timestamp;

    @JsonIgnore
    private String transaction;

    @JsonIgnore
    private boolean matched;

    public VegetableData getBefore() {
        return before;
    }

    public void setBefore(VegetableData before) {
        this.before = before;
    }

    public VegetableData getAfter() {
        return after;
    }

    public void setAfter(VegetableData after) {
        this.after = after;
    }

    public SourceData getSource() {
        return source;
    }

    public void setSource(SourceData source) {
        this.source = source;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    @Override
    public String toString() {
        return "VegetableEvent [before=" + before + ", after=" + after + ", source=" + source + ", operation=" + operation + ", timestamp=" + timestamp + "]";
    }
}
