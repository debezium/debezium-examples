# Switch to this database
USE inventory;

DROP TABLE orders;

CREATE TABLE categories (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  average_price BIGINT,
  PRIMARY KEY (id)
) AUTO_INCREMENT = 100001;

INSERT INTO categories VALUES (default, 'Toys', 4500);
INSERT INTO categories VALUES (default, 'Books', 2200);
INSERT INTO categories VALUES (default, 'Computers', 6700);
INSERT INTO categories VALUES (default, 'Tools', 4800);
INSERT INTO categories VALUES (default, 'Plants', 1900);
INSERT INTO categories VALUES (default, 'Food', 500);
INSERT INTO categories VALUES (default, 'Furniture', 2700);
INSERT INTO categories VALUES (default, 'Cloth', 3700);

CREATE TABLE orders (
  id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ts TIMESTAMP NOT NULL,
  purchaser_id INTEGER NOT NULL,
  product_id INTEGER NOT NULL,
  category_id BIGINT NOT NULL,
  quantity INTEGER NOT NULL,
  sales_price BIGINT NOT NULL
) AUTO_INCREMENT = 100001;

ALTER TABLE orders ADD CONSTRAINT fk_orders_product_id FOREIGN KEY (product_id) REFERENCES inventory.products(id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_purchaser_id FOREIGN KEY (purchaser_id) REFERENCES inventory.customers(id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_category_id FOREIGN KEY (category_id) REFERENCES inventory.categories(id);
