-- WAL lets the connector read while the application keeps writing
PRAGMA journal_mode = WAL;

CREATE TABLE customers (
  id INTEGER NOT NULL PRIMARY KEY,
  first_name TEXT NOT NULL,
  last_name TEXT NOT NULL,
  email TEXT NOT NULL UNIQUE
);

INSERT INTO customers VALUES (1001, 'Sally', 'Thomas', 'sally.thomas@acme.com');
INSERT INTO customers VALUES (1002, 'George', 'Bailey', 'gbailey@foobar.com');
INSERT INTO customers VALUES (1003, 'Edward', 'Walker', 'ed@walker.com');
INSERT INTO customers VALUES (1004, 'Anne', 'Kretchmar', 'annek@noanswer.org');
