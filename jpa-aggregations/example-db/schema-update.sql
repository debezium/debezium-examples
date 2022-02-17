# Switch to this database
USE inventory;

CREATE TABLE categories (
  id INTEGER NOT NULL,
  name VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

INSERT INTO categories VALUES (1000001, 'Premium');

ALTER TABLE customers ADD some_blob BLOB;
ALTER TABLE customers ADD isactive BOOLEAN;
ALTER TABLE customers ADD category_id INTEGER;
ALTER TABLE customers ADD birthday DATE;
ALTER TABLE customers ADD CONSTRAINT fk_customer_category_id FOREIGN KEY (category_id) REFERENCES inventory.categories(id);

UPDATE customers SET isactive = 1;
UPDATE customers SET category_id = 1000001 WHERE id = 1001;
UPDATE customers SET birthday = '1978-5-31' WHERE id = 1001;

CREATE TABLE customer_scores (
  customer_id INTEGER NOT NULL,
  idx TINYINT NOT NULL,
  score DECIMAL(4,2) NOT NULL,
  PRIMARY KEY (customer_id, score)
);

CREATE TABLE customer_tags (
  customer_id INTEGER NOT NULL,
  idx TINYINT NOT NULL,
  tag VARCHAR(255) NOT NULL,
  PRIMARY KEY (customer_id, tag)
);

INSERT INTO customer_scores VALUES (1001, 0, 8.9);
INSERT INTO customer_scores VALUES (1001, 1, 42.0);

INSERT INTO customer_tags VALUES (1001, 0, 'foo');
INSERT INTO customer_tags VALUES (1001, 1, 'bar');
INSERT INTO customer_tags VALUES (1001, 2, 'qux');

CREATE TABLE aggregates (
  rootId VARCHAR(2000) NOT NULL,
  rootType VARCHAR(255) NOT NULL,
  keySchema LONGTEXT,
  valueSchema LONGTEXT,
  materialization LONGTEXT,
  PRIMARY KEY (rootId, rootType)
) CHARSET=latin1;
