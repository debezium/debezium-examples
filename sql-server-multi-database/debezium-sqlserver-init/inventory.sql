-- ==========================================
-- Database 1: testDB1 (Products)
-- ==========================================
CREATE DATABASE testDB1;
GO
USE testDB1;
EXEC sys.sp_cdc_enable_db;

CREATE TABLE products (
  id INTEGER IDENTITY(101,1) NOT NULL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(512),
  weight FLOAT
);
INSERT INTO products(name,description,weight)
  VALUES ('scooter','Small 2-wheel scooter',3.14);
INSERT INTO products(name,description,weight)
  VALUES ('car battery','12V car battery',8.1);
INSERT INTO products(name,description,weight)
  VALUES ('12-pack drill bits','12-pack of drill bits with sizes ranging from #40 to #3',0.8);
INSERT INTO products(name,description,weight)
  VALUES ('hammer','12oz carpenter''s hammer',0.75);
INSERT INTO products(name,description,weight)
  VALUES ('hammer','14oz carpenter''s hammer',0.875);
INSERT INTO products(name,description,weight)
  VALUES ('jacket','water resistent black wind breaker',0.1);
EXEC sys.sp_cdc_enable_table @source_schema = 'dbo', @source_name = 'products', @role_name = NULL, @supports_net_changes = 0;
GO

-- ==========================================
-- Database 2: testDB2 (Customers & Orders)
-- ==========================================
CREATE DATABASE testDB2;
GO
USE testDB2;
EXEC sys.sp_cdc_enable_db;

CREATE TABLE customers (
  id INTEGER IDENTITY(1001,1) NOT NULL PRIMARY KEY,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE
);
INSERT INTO customers(first_name,last_name,email)
  VALUES ('Sally','Thomas','sally.thomas@acme.com');
INSERT INTO customers(first_name,last_name,email)
  VALUES ('George','Bailey','gbailey@foobar.com');
INSERT INTO customers(first_name,last_name,email)
  VALUES ('Edward','Walker','ed@walker.com');
INSERT INTO customers(first_name,last_name,email)
  VALUES ('Anne','Kretchmar','annek@noanswer.org');
EXEC sys.sp_cdc_enable_table @source_schema = 'dbo', @source_name = 'customers', @role_name = NULL, @supports_net_changes = 0;

CREATE TABLE orders (
  id INTEGER IDENTITY(10001,1) NOT NULL PRIMARY KEY,
  order_date DATE NOT NULL,
  purchaser INTEGER NOT NULL,
  quantity INTEGER NOT NULL,
  product_id INTEGER NOT NULL,
  FOREIGN KEY (purchaser) REFERENCES customers(id)
);
INSERT INTO orders(order_date,purchaser,quantity,product_id)
  VALUES ('16-JAN-2016', 1001, 1, 102);
INSERT INTO orders(order_date,purchaser,quantity,product_id)
  VALUES ('17-JAN-2016', 1002, 2, 105);
INSERT INTO orders(order_date,purchaser,quantity,product_id)
  VALUES ('19-FEB-2016', 1002, 2, 106);
INSERT INTO orders(order_date,purchaser,quantity,product_id)
  VALUES ('21-FEB-2016', 1003, 1, 107);
EXEC sys.sp_cdc_enable_table @source_schema = 'dbo', @source_name = 'orders', @role_name = NULL, @supports_net_changes = 0;
GO
