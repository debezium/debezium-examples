CREATE SCHEMA inventory;

CREATE TABLE inventory.customers (
    id INT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL
);

CREATE TABLE inventory.products (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    description TEXT,
    weight BIGINT
);

CREATE TABLE inventory.orders (
    id INT PRIMARY KEY,
    order_date date NOT NULL,
    purchaser integer NOT NULL,
    quantity integer NOT NULL,
    product_id integer NOT NULL,
    FOREIGN KEY (purchaser) REFERENCES inventory.customers(id),
    FOREIGN KEY (product_id) REFERENCES inventory.products(id)
);

INSERT INTO inventory.customers (id, first_name, last_name, email) VALUES
(1,'Alice', 'Smith', 'alice.smith@example.com'),
(2,'Bob', 'Johnson', 'bob.johnson@example.com'),
(3,'Charlie', 'Brown', 'charlie.brown@example.com');

INSERT INTO inventory.products (id, name, description, weight) VALUES
(1, 'Laptop', 'High-performance ultrabook', 1250),
(2, 'Smartphone', 'Latest model with AMOLED display', 180),
(3, 'Coffee Mug', 'Ceramic mug with lid', 350);

INSERT INTO inventory.orders (id, order_date, purchaser, quantity, product_id) VALUES
(1,'2025-07-01', 1, 1, 2),
(2,'2025-07-02', 2, 2, 1),
(3,'2025-07-03', 3, 1, 3);