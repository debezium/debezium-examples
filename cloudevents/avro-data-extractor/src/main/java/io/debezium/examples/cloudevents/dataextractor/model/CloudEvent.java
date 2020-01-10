package io.debezium.examples.cloudevents.dataextractor.model;

public class CloudEvent {

    public String id;
    public String source;
    public String specversion;
    public String type;
    public String time;
    public String datacontenttype;
    public String dataschema;
    public String iodebeziumop;
    public String iodebeziumversion;
    public String iodebeziumconnector;
    public String iodebeziumname;
    public String iodebeziumtsms;
    public boolean iodebeziumsnapshot;
    public String iodebeziumdb;
    public String iodebeziumschema;
    public String iodebeziumtable;
    public String iodebeziumtxId;
    public String iodebeziumlsn;
    public String iodebeziumxmin;
    public byte[] data;
}
