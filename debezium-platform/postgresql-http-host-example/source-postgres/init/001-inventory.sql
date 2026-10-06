-- Copyright Debezium Authors.
--
-- Licensed under the Apache License version 2.0, available at
-- http://www.apache.org/licenses/LICENSE-2.0

CREATE USER debezium WITH REPLICATION LOGIN PASSWORD 'dbz';

CREATE SCHEMA inventory;
CREATE TABLE inventory.customers (
    id SERIAL PRIMARY KEY,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE
);

INSERT INTO inventory.customers (first_name, last_name, email)
VALUES ('Anne', 'Kretchmar', 'anne@example.com');

GRANT USAGE ON SCHEMA inventory TO debezium;
GRANT SELECT ON ALL TABLES IN SCHEMA inventory TO debezium;
ALTER DEFAULT PRIVILEGES IN SCHEMA inventory GRANT SELECT ON TABLES TO debezium;

CREATE PUBLICATION dbz_host_example FOR TABLE inventory.customers;
