-- This version is used with the Debezium JDBC Sink Connector
CREATE TABLE dbserver1_inventory_vegetable_enriched (
    -- Business Data
    id INTEGER,
    name VARCHAR(255) NULL,
    description TEXT NULL,
    
    -- Audit Metadata
    tx_id VARCHAR(255),
    user_name VARCHAR(255) NULL,
    usecase VARCHAR(255) NULL,
    client_date BIGINT NULL,
    
    -- Change Metadata (for uniqueness)
    op VARCHAR(1),
    lsn BIGINT,
    ts_ms BIGINT,

    -- Kafka Metadata to prevent squashing of events
    __connect_partition INTEGER,
    __connect_offset BIGINT,
    __connect_topic VARCHAR(255),
    
    -- To ensure that all events are kept we will use kafka metadata to 
    -- identify unique rows
PRIMARY KEY (__connect_offset, __connect_partition, __connect_topic)
);

