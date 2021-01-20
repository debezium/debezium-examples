CREATE TABLE products
(
    id          INT(11)      NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    weight      FLOAT        DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE products_on_hand
(
    product_id INT(11) NOT NULL,
    quantity   INT(11) NOT NULL,
    PRIMARY KEY (product_id)
) ENGINE = InnoDB;

CREATE TABLE orders
(
    order_number INT(11) NOT NULL,
    order_date   DATE    NOT NULL,
    purchaser    INT(11) NOT NULL,
    quantity     INT(11) NOT NULL,
    product_id   INT(11) NOT NULL,
    PRIMARY KEY (order_number)
) ENGINE = InnoDB;
