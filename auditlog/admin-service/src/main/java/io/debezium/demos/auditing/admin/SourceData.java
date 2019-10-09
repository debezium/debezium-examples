package io.debezium.demos.auditing.admin;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SourceData {
    
    private String version;    
    private String connector;    
    private String name;
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    @JsonProperty("ts_ms")
    private Date timestamp;    
    private String snapshot;    
    private String db;    
    private String schema;    
    private String table;
    @JsonProperty("txId")
    private Long transactionId;    
    private Long lsn;    
    private String xmin;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getConnector() {
        return connector;
    }

    public void setConnector(String connector) {
        this.connector = connector;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(String snapshot) {
        this.snapshot = snapshot;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getLsn() {
        return lsn;
    }

    public void setLsn(Long lsn) {
        this.lsn = lsn;
    }

    public String getXmin() {
        return xmin;
    }

    public void setXmin(String xmin) {
        this.xmin = xmin;
    }

    @Override
    public String toString() {
        return "SourceData [version=" + version + ", connector=" + connector + ", name=" + name + ", timestamp=" + timestamp + ", snapshot=" + snapshot + ", db=" + db + ", schema=" + schema + ", table=" + table +
               ", transactionId=" + transactionId + ", lsn=" + lsn + ", xmin=" + xmin + "]";
    }

}
