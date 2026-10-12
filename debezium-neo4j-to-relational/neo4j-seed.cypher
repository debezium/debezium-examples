// Node key constraints give each label a key, which the Neo4j CDC events expose in their `keys`
// map. The Neo4jDebeziumConverter SMT reads that to derive the relational primary key (and the
// endpoint keys that become foreign-key / join-table columns), so the constraints must exist
// *before* the data is created. A plain uniqueness constraint is NOT enough: Neo4j only populates
// the CDC `keys` field from node *key* constraints (an Enterprise feature).
CREATE CONSTRAINT customer_id IF NOT EXISTS FOR (c:Customer) REQUIRE c.id IS NODE KEY;
CREATE CONSTRAINT product_id IF NOT EXISTS FOR (p:Product) REQUIRE p.id IS NODE KEY;
CREATE CONSTRAINT order_id IF NOT EXISTS FOR (o:Order) REQUIRE o.id IS NODE KEY;

// Property names are camelCase on purpose: the SMT runs with column.naming=snake_case, so
// `firstName` becomes the `first_name` column in PostgreSQL.
CREATE (:Customer {id: 1001, firstName: 'Sally',  lastName: 'Thomas',    email: 'sally.thomas@acme.com'});
CREATE (:Customer {id: 1002, firstName: 'George', lastName: 'Bailey',    email: 'gbailey@foobar.com'});
CREATE (:Customer {id: 1003, firstName: 'Edward', lastName: 'Walker',    email: 'ed@walker.com'});
CREATE (:Customer {id: 1004, firstName: 'Anne',   lastName: 'Kretchmar', email: 'annek@noanswer.org'});

CREATE (:Product {id: 200, name: 'Widget', price: 19.99});
CREATE (:Product {id: 201, name: 'Gadget', price: 29.99});
CREATE (:Product {id: 202, name: 'Gizmo',  price: 9.99});

CREATE (:Order {id: 5001, total: 39.98, status: 'pending'});
CREATE (:Order {id: 5002, total: 29.99, status: 'shipped'});

// (:Order)-[:PLACED_BY]->(:Customer) -> foreign key orders.customer_id
MATCH (o:Order {id: 5001}), (c:Customer {id: 1001}) CREATE (o)-[:PLACED_BY]->(c);
MATCH (o:Order {id: 5002}), (c:Customer {id: 1002}) CREATE (o)-[:PLACED_BY]->(c);

// (:Order)-[:CONTAINS {quantity}]->(:Product) -> join table order_items(order_id, product_id, quantity)
MATCH (o:Order {id: 5001}), (p:Product {id: 200}) CREATE (o)-[:CONTAINS {quantity: 2}]->(p);
MATCH (o:Order {id: 5002}), (p:Product {id: 201}) CREATE (o)-[:CONTAINS {quantity: 1}]->(p);
