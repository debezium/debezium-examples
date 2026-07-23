CREATE TABLE inventory.addresses (
  id SERIAL NOT NULL PRIMARY KEY,
  customer_id INTEGER NOT NULL,
  street VARCHAR(255) NOT NULL,
  city VARCHAR(255) NOT NULL,
  zipcode VARCHAR(255) NOT NULL,
  country VARCHAR(255) NOT NULL,
  FOREIGN KEY (customer_id) REFERENCES inventory.customers(id)
);
ALTER SEQUENCE inventory.addresses_id_seq RESTART WITH 100001;
ALTER TABLE inventory.addresses REPLICA IDENTITY FULL;

INSERT INTO inventory.addresses
VALUES (default, 1001, '42 Main Street', 'Hamburg', '90210', 'Canada'),
       (default, 1001, '11 Post Dr.', 'Berlin', '90211', 'Canada'),
       (default, 1002, '12 Rodeo Dr.', 'Los Angeles', '90212', 'US'),
       (default, 1002, '1 Debezium Plaza', 'Monterey', '90213', 'US'),
       (default, 1002, '2 Debezium Plaza', 'Monterey', '90213', 'US');
