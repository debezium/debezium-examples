CREATE SCHEMA inventorysink;
SET SCHEMA 'inventorysink';

CREATE TABLE customers (
  id SERIAL NOT NULL PRIMARY KEY,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  biography TEXT
);

CREATE OR REPLACE FUNCTION ignore_unchanged_biography()
  RETURNS TRIGGER AS
$BODY$
BEGIN
  IF NEW."biography" = '__debezium_unavailable_value'
  THEN
    NEW."biography" = OLD."biography";
  END IF;

  RETURN NEW;
END;
$BODY$ LANGUAGE PLPGSQL;

CREATE TRIGGER customer_biography_trigger
BEFORE UPDATE OF "biography"
  ON customers
FOR EACH ROW
EXECUTE PROCEDURE ignore_unchanged_biography();
