CREATE KEYSPACE testdb
WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3}
AND durable_writes = true;

USE testdb;

CREATE TABLE products(
  id bigint PRIMARY KEY,
  name text,
  description text,
  weight float
) WITH cdc=true;

INSERT INTO products(id,name,description,weight)
    VALUES (101,'scooter','Small 2-wheel scooter',3.14);
INSERT INTO products(id,name,description,weight)
  VALUES (102,'car battery','12V car battery',8.1);
INSERT INTO products(id,name,description,weight)
  VALUES (103,'12-pack drill bits','12-pack of drill bits with sizes ranging from #40 to #3',0.8);
INSERT INTO products(id,name,description,weight)
  VALUES (104,'hammer','12oz carpenter''s hammer',0.75);
INSERT INTO products(id,name,description,weight)
  VALUES (105,'hammer','14oz carpenter''s hammer',0.875);
INSERT INTO products(id,name,description,weight)
  VALUES (105,'hammer','16oz carpenter''s hammer',1.0);
INSERT INTO products(id,name,description,weight)
  VALUES (106,'rocks','box of assorted rocks',5.3);
INSERT INTO products(id,name,description,weight)
  VALUES (107,'jacket','water resistent black wind breaker',0.1);
INSERT INTO products(id,name,description,weight)
  VALUES (108,'spare tire','24 inch spare tire',22.2);

CREATE TABLE products_on_hand (
  product_id bigint PRIMARY KEY,
  quantity bigint,
) WITH cdc=true;

INSERT INTO products_on_hand(product_id, quantity) VALUES (101,3);
INSERT INTO products_on_hand(product_id, quantity) VALUES (102,8);
INSERT INTO products_on_hand(product_id, quantity) VALUES (103,18);
INSERT INTO products_on_hand(product_id, quantity) VALUES (104,4);
INSERT INTO products_on_hand(product_id, quantity) VALUES (105,5);
INSERT INTO products_on_hand(product_id, quantity) VALUES (106,0);
INSERT INTO products_on_hand(product_id, quantity) VALUES (107,44);
INSERT INTO products_on_hand(product_id, quantity) VALUES (108,2);
INSERT INTO products_on_hand(product_id, quantity) VALUES (109,5);

CREATE TABLE customers (
  id bigint PRIMARY KEY,
  first_name varchar,
  last_name varchar,
  email varchar,
) WITH cdc=true;

INSERT INTO customers(id,first_name,last_name,email)
  VALUES (1,'Sally','Thomas','sally.thomas@acme.com');
INSERT INTO customers(id,first_name,last_name,email)
  VALUES (2,'George','Bailey','gbailey@foobar.com');
INSERT INTO customers(id,first_name,last_name,email)
  VALUES (3,'Edward','Walker','ed@walker.com');
INSERT INTO customers(id,first_name,last_name,email)
  VALUES (4,'Anne','Kretchmar','annek@noanswer.org');


