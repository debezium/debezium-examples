INSERT INTO products(name, description, weight)
VALUES ('scooter', 'Small 2-wheel scooter', 3.14);

INSERT INTO products(name, description, weight)
VALUES ('car battery', '12V car battery', 8.1);

INSERT INTO products(name, description, weight)
VALUES ('12-pack drill bits', '12-pack of drill bits with sizes ranging from #40 to #3', 0.8);

INSERT INTO products(name, description, weight)
VALUES ('hammer', '12oz carpenter''s hammer', 0.75);

INSERT INTO products(name, description, weight)
VALUES ('hammer', '14oz carpenter''s hammer', 0.875);

INSERT INTO products(name, description, weight)
VALUES ('hammer', '16oz carpenter''s hammer', 1.0);

INSERT INTO products(name, description, weight)
VALUES ('rocks', 'box of assorted rocks', 5.3);

INSERT INTO products(name, description, weight)
VALUES ('jacket', 'water resistent black wind breaker', 0.1);

INSERT INTO products(name, description, weight)
VALUES ('spare tire', '24 inch spare tire', 22.2);

INSERT INTO products_on_hand(product_id, quantity)
VALUES (1001, 3);

INSERT INTO products_on_hand(product_id, quantity)
VALUES (1002, 8);

INSERT INTO products_on_hand(product_id, quantity)
VALUES (1003, 18);

INSERT INTO products_on_hand(product_id, quantity)
VALUES (1004, 4);

INSERT INTO products_on_hand(product_id, quantity)
VALUES (1005, 5);

INSERT INTO products_on_hand(product_id, quantity)
VALUES (1006, 0);

INSERT INTO products_on_hand(product_id, quantity)
VALUES (1007, 44);

INSERT INTO products_on_hand(product_id, quantity)
VALUES (1008, 2);

INSERT INTO products_on_hand(product_id, quantity)
VALUES (1009, 5);

INSERT INTO orders(order_date, purchaser, quantity, product_id)
VALUES ('2016-01-16', 1001, 1, 1002);

INSERT INTO orders(order_date, purchaser, quantity, product_id)
VALUES ('2016-01-17', 1002, 2, 1005);

INSERT INTO orders(order_date, purchaser, quantity, product_id)
VALUES ('2016-02-19', 1002, 2, 1006);

INSERT INTO orders(order_date, purchaser, quantity, product_id)
VALUES ('2016-02-21', 1003, 1, 1007);
