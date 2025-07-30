CREATE SCHEMA inventory;

CREATE TABLE products (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    description TEXT,
    weight BIGINT
);


INSERT INTO products (id, name, description, weight) VALUES
(1, 'Laptop', 'High-performance ultrabook', 1250),
(2, 'Smartphone', 'Latest model with AMOLED display', 180),
(3, 'Coffee Mug', 'Ceramic mug with lid', 350);