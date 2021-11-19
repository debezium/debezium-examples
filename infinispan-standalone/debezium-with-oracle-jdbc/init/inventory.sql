-- Create some customers
CREATE TABLE customers (
    id NUMBER(4) NOT NULL PRIMARY KEY,
    first_name VARCHAR2(255) NOT NULL,
    last_name VARCHAR2(255) NOT NULL,
    email VARCHAR2(255) NOT NULL UNIQUE
);

GRANT SELECT ON customers to c##dbzuser;
ALTER TABLE customers ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;

INSERT INTO customers VALUES (1001, 'Sally', 'Thomas', 'sally.thomas@acme.com');
INSERT INTO customers VALUES (1002, 'George', 'Bailey', 'gbailey@foobar.com');
INSERT INTO customers VALUES (1003, 'Edward', 'Walker', 'ed@walker.com');
INSERT INTO customers VALUES (1004, 'Anne', 'Kretchmar', 'annek@noanswer.org');

