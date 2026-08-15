-- Create tables and populate with sample data for CDC
CREATE TABLE customers (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

-- Debezium PostgreSQL connector requires replica identity set to FULL to capture updates/deletes fully
ALTER TABLE customers REPLICA IDENTITY FULL;

-- Populate with mock data
INSERT INTO customers (first_name, last_name, email) VALUES
('Sally', 'Thomas', 'sally.thomas@acme.com'),
('George', 'Bailey', 'gbailey@foobar.com'),
('Edward', 'Walker', 'ed@walker.com');
