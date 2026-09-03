-- ksqlDB split step for the Neo4jCudConverter SMT (output.mode=array).
--
-- Each Debezium topic carries a JSON array of CUD events per change event. ksqlDB
-- EXPLODE splits each array into individual records (order preserved) and writes
-- them to the neo4j.* topics the Neo4j sink connector consumes.
--
SET 'auto.offset.reset' = 'earliest';

-- ---------- customers ----------
CREATE STREAM IF NOT EXISTS customers_cud_arrays (cud_events ARRAY<VARCHAR>)
  WITH (KAFKA_TOPIC='dbserver1.public.customers', VALUE_FORMAT='JSON', WRAP_SINGLE_VALUE=false);

CREATE STREAM IF NOT EXISTS customers_cud_events
  WITH (KAFKA_TOPIC='neo4j.customers', VALUE_FORMAT='KAFKA', PARTITIONS=1) AS
  SELECT EXPLODE(cud_events) AS cud_event
  FROM customers_cud_arrays
  EMIT CHANGES;

-- ---------- products ----------
CREATE STREAM IF NOT EXISTS products_cud_arrays (cud_events ARRAY<VARCHAR>)
  WITH (KAFKA_TOPIC='dbserver1.public.products', VALUE_FORMAT='JSON', WRAP_SINGLE_VALUE=false);

CREATE STREAM IF NOT EXISTS products_cud_events
  WITH (KAFKA_TOPIC='neo4j.products', VALUE_FORMAT='KAFKA', PARTITIONS=1) AS
  SELECT EXPLODE(cud_events) AS cud_event
  FROM products_cud_arrays
  EMIT CHANGES;

-- ---------- orders ----------
CREATE STREAM IF NOT EXISTS orders_cud_arrays (cud_events ARRAY<VARCHAR>)
  WITH (KAFKA_TOPIC='dbserver1.public.orders', VALUE_FORMAT='JSON', WRAP_SINGLE_VALUE=false);

CREATE STREAM IF NOT EXISTS orders_cud_events
  WITH (KAFKA_TOPIC='neo4j.orders', VALUE_FORMAT='KAFKA', PARTITIONS=1) AS
  SELECT EXPLODE(cud_events) AS cud_event
  FROM orders_cud_arrays
  EMIT CHANGES;

-- ---------- order_items ----------
CREATE STREAM IF NOT EXISTS order_items_cud_arrays (cud_events ARRAY<VARCHAR>)
  WITH (KAFKA_TOPIC='dbserver1.public.order_items', VALUE_FORMAT='JSON', WRAP_SINGLE_VALUE=false);

CREATE STREAM IF NOT EXISTS order_items_cud_events
  WITH (KAFKA_TOPIC='neo4j.order_items', VALUE_FORMAT='KAFKA', PARTITIONS=1) AS
  SELECT EXPLODE(cud_events) AS cud_event
  FROM order_items_cud_arrays
  EMIT CHANGES;
