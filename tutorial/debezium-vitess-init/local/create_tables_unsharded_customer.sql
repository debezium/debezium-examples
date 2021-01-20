CREATE TABLE my_product_seq
(
    id      INT,
    next_id BIGINT,
    cache   BIGINT,
    PRIMARY KEY (id)
) comment 'vitess_sequence';

INSERT INTO my_product_seq(id, next_id, cache)
VALUES (0, 1000, 100);

CREATE TABLE my_order_seq
(
    id      INT,
    next_id BIGINT,
    cache   BIGINT,
    PRIMARY KEY (id)
) comment 'vitess_sequence';

INSERT INTO my_order_seq(id, next_id, cache)
VALUES (0, 1000, 100);

CREATE TABLE my_customer_seq
(
    id      INT,
    next_id BIGINT,
    cache   BIGINT,
    PRIMARY KEY (id)
) comment 'vitess_sequence';

INSERT INTO my_customer_seq(id, next_id, cache)
VALUES (0, 1000, 100);

CREATE TABLE customers
(
    id         INT(11) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name  VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;
