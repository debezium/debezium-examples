CREATE SCHEMA IF NOT EXISTS inventory;

CREATE TABLE inventory.item (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL
);

ALTER TABLE inventory.item REPLICA IDENTITY FULL;

INSERT INTO inventory.item (id, name, price) VALUES (1, 'Notebook', 10.99);
INSERT INTO inventory.item (id, name, price) VALUES (2, 'Wireless Mouse', 25.50);
