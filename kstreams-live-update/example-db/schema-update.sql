
set search_path='inventory';

DROP TABLE IF EXISTS orders;

CREATE TABLE categories (
  id serial primary key,
  name text NOT NULL,
  average_price int8
);

INSERT INTO categories (name, average_price) VALUES ('Toys', 4500);
INSERT INTO categories (name, average_price) VALUES ('Books', 2200);
INSERT INTO categories (name, average_price) VALUES ('Computers', 6700);
INSERT INTO categories (name, average_price) VALUES ('Tools', 4800);
INSERT INTO categories (name, average_price) VALUES ('Plants', 1900);
INSERT INTO categories (name, average_price) VALUES ('Food', 500);
INSERT INTO categories (name, average_price) VALUES ('Furniture', 2700);
INSERT INTO categories (name, average_price) VALUES ('Cloth', 3700);

CREATE TABLE orders (
  id serial PRIMARY KEY,
  ts TIMESTAMP NOT NULL,
  purchaser_id int4 NOT NULL,
  product_id int4 NOT NULL,
  category_id int8 NOT NULL,
  quantity int4 NOT NULL,
  sales_price int8  NOT NULL
);

ALTER TABLE orders ADD CONSTRAINT fk_orders_product_id FOREIGN KEY (product_id) REFERENCES inventory.products(id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_purchaser_id FOREIGN KEY (purchaser_id) REFERENCES inventory.customers(id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_category_id FOREIGN KEY (category_id) REFERENCES inventory.categories(id);
