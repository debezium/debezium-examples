
-- Create and populate our products using a single insert with many rows
CREATE TABLE ##LIBRARY##.PRODUCTS (
  id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY
      (START WITH 101, INCREMENT BY 1) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(512),
  weight FLOAT
);
INSERT INTO ##LIBRARY##.PRODUCTS(name,description,weight)
  VALUES ('scooter','Small 2-wheel scooter',3.14);
INSERT INTO ##LIBRARY##.PRODUCTS(name,description,weight)
  VALUES ('car battery','12V car battery',8.1);
INSERT INTO ##LIBRARY##.PRODUCTS(name,description,weight)
  VALUES ('12-pack drill bits','12-pack of drill bits with sizes ranging from #40 to #3',0.8);
INSERT INTO ##LIBRARY##.PRODUCTS(name,description,weight)
  VALUES ('hammer','12oz carpenter''s hammer',0.75);
INSERT INTO ##LIBRARY##.PRODUCTS(name,description,weight)
  VALUES ('hammer','14oz carpenter''s hammer',0.875);
INSERT INTO ##LIBRARY##.PRODUCTS(name,description,weight)
  VALUES ('hammer','16oz carpenter''s hammer',1.0);
INSERT INTO ##LIBRARY##.PRODUCTS(name,description,weight)
  VALUES ('rocks','box of assorted rocks',5.3);
INSERT INTO ##LIBRARY##.PRODUCTS(name,description,weight)
  VALUES ('jacket','water resistent black wind breaker',0.1);
INSERT INTO ##LIBRARY##.PRODUCTS(name,description,weight)
  VALUES ('spare tire','24 inch spare tire',22.2);

CREATE TABLE ##LIBRARY##.PRODUCTS_ON_HAND (
  product_id INTEGER NOT NULL PRIMARY KEY,
  quantity INTEGER NOT NULL,
  FOREIGN KEY (product_id) REFERENCES ##LIBRARY##.PRODUCTS(id)
);
INSERT INTO ##LIBRARY##.PRODUCTS_ON_HAND VALUES (101,3);
INSERT INTO ##LIBRARY##.PRODUCTS_ON_HAND VALUES (102,8);
INSERT INTO ##LIBRARY##.PRODUCTS_ON_HAND VALUES (103,18);
INSERT INTO ##LIBRARY##.PRODUCTS_ON_HAND VALUES (104,4);
INSERT INTO ##LIBRARY##.PRODUCTS_ON_HAND VALUES (105,5);
INSERT INTO ##LIBRARY##.PRODUCTS_ON_HAND VALUES (106,0);
INSERT INTO ##LIBRARY##.PRODUCTS_ON_HAND VALUES (107,44);
INSERT INTO ##LIBRARY##.PRODUCTS_ON_HAND VALUES (108,2);
INSERT INTO ##LIBRARY##.PRODUCTS_ON_HAND VALUES (109,5);

CREATE TABLE ##LIBRARY##.CUSTOMERS (
  id INTEGER  NOT NULL GENERATED ALWAYS AS IDENTITY
      (START WITH 1001, INCREMENT BY 1) PRIMARY KEY,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE
);
INSERT INTO ##LIBRARY##.CUSTOMERS(first_name,last_name,email)
  VALUES ('Sally','Thomas','sally.thomas@acme.com');
INSERT INTO ##LIBRARY##.CUSTOMERS(first_name,last_name,email)
  VALUES ('George','Bailey','gbailey@foobar.com');
INSERT INTO ##LIBRARY##.CUSTOMERS(first_name,last_name,email)
  VALUES ('Edward','Walker','ed@walker.com');
INSERT INTO ##LIBRARY##.CUSTOMERS(first_name,last_name,email)
  VALUES ('Anne','Kretchmar','annek@noanswer.org');

CREATE TABLE ##LIBRARY##.ORDERS (
  id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY
      (START WITH 10001, INCREMENT BY 1) PRIMARY KEY,
  order_date DATE NOT NULL,
  purchaser INTEGER NOT NULL,
  quantity INTEGER NOT NULL,
  product_id INTEGER NOT NULL,
  FOREIGN KEY (purchaser) REFERENCES ##LIBRARY##.CUSTOMERS(id),
  FOREIGN KEY (product_id) REFERENCES ##LIBRARY##.PRODUCTS(id)
);
INSERT INTO ##LIBRARY##.ORDERS(order_date,purchaser,quantity,product_id)
  VALUES ('2016-01-16', 1001, 1, 102);
INSERT INTO ##LIBRARY##.ORDERS(order_date,purchaser,quantity,product_id)
  VALUES ('2016-01-17', 1002, 2, 105);
INSERT INTO ##LIBRARY##.ORDERS(order_date,purchaser,quantity,product_id)
  VALUES ('2016-02-19', 1002, 2, 106);
INSERT INTO ##LIBRARY##.ORDERS(order_date,purchaser,quantity,product_id)
  VALUES ('2016-02-21', 1003, 1, 107);
