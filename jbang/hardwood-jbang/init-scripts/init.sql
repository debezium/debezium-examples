CREATE SCHEMA inventory;

CREATE TABLE inventory.customers (
    id INT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL
);

INSERT INTO inventory.customers (id, first_name, last_name, email) VALUES
(1,'Alice', 'Smith', 'alice.smith@example.com'),
(2,'Bob', 'Johnson', 'bob.johnson@example.com'),
(3,'Charlie', 'Brown', 'charlie.brown@example.com');
