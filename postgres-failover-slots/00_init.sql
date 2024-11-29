BEGIN;

create user replicator with replication encrypted password 'zufsob-kuvtum-bImxa6';
SELECT pg_create_physical_replication_slot('replication_slot');

CREATE SCHEMA inventory;

-- customers
CREATE TABLE inventory.customers (
  id SERIAL NOT NULL PRIMARY KEY,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  is_test_account BOOLEAN NOT NULL
);

ALTER SEQUENCE inventory.customers_id_seq RESTART WITH 1001;
ALTER TABLE inventory.customers REPLICA IDENTITY FULL;

INSERT INTO inventory.customers
VALUES (default, 'Sally', 'Thomas', 'sally.thomas@acme.com', FALSE),
       (default, 'George', 'Bailey', 'gbailey@foobar.com', FALSE),
       (default, 'Edward', 'Walker', 'ed@walker.com', FALSE),
       (default, 'Aidan', 'Barrett', 'aidan@example.com', TRUE),
       (default, 'Anne', 'Kretchmar', 'annek@noanswer.org', TRUE),
       (default, 'Melissa', 'Cole', 'melissa@example.com', FALSE),
       (default, 'Rosalie', 'Stewart', 'rosalie@example.com', FALSE);

COMMIT;
