CREATE TABLE customers (
  id INTEGER NOT NULL PRIMARY KEY,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL
);

CREATE TABLE products (
  id INTEGER NOT NULL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  price NUMERIC(10, 2) NOT NULL
);

CREATE TABLE orders (
  id INTEGER NOT NULL PRIMARY KEY,
  customer_id INTEGER NOT NULL,
  total NUMERIC(10, 2) NOT NULL,
  status VARCHAR(32) NOT NULL,
  FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE TABLE order_items (
  order_id INTEGER NOT NULL,
  product_id INTEGER NOT NULL,
  quantity INTEGER NOT NULL,
  PRIMARY KEY (order_id, product_id),
  FOREIGN KEY (order_id) REFERENCES orders(id),
  FOREIGN KEY (product_id) REFERENCES products(id)
);

-- REPLICA IDENTITY FULL so DELETE change events carry the key columns the SMT
-- needs to build the CUD `ids` block for delete operations.
ALTER TABLE customers REPLICA IDENTITY FULL;
ALTER TABLE products REPLICA IDENTITY FULL;
ALTER TABLE orders REPLICA IDENTITY FULL;
ALTER TABLE order_items REPLICA IDENTITY FULL;

INSERT INTO customers (id, first_name, last_name, email) VALUES
  (1001, 'Sally', 'Thomas', 'sally.thomas@acme.com'),
  (1002, 'George', 'Bailey', 'gbailey@foobar.com'),
  (1003, 'Edward', 'Walker', 'ed@walker.com'),
  (1004, 'Anne', 'Kretchmar', 'annek@noanswer.org');

INSERT INTO products (id, name, price) VALUES
  (200, 'Widget', 19.99),
  (201, 'Gadget', 29.99),
  (202, 'Gizmo', 9.99);

INSERT INTO orders (id, customer_id, total, status) VALUES
  (5001, 1001, 39.98, 'pending'),
  (5002, 1002, 29.99, 'shipped');

INSERT INTO order_items (order_id, product_id, quantity) VALUES
  (5001, 200, 2),
  (5002, 201, 1);
