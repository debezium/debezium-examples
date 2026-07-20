-- setup.sql — provision the "ecommerce" database on AWS RDS for PostgreSQL.
--
-- Mirrors the container bootstrap in docker-compose.yaml and the schema in
-- internal/database/postgres/ddl.go, adapted for RDS:
--   * app_owner  — owns the schema + tables; the app connects as this user
--                  (POSTGRES_URI = postgres://app_owner:app_owner@.../ecommerce)
--   * debezium   — replication + read-only SELECT, for the CDC connector
--
-- Run from your laptop through the SSH tunnel, connected to any existing DB
-- (the RDS default/maintenance db) as the master user:
--
--   ssh -N -L 5432:<rds-endpoint>:5432 dmp-demo        # terminal 1
--   psql -h localhost -p 5432 -U <master-user> -d postgres -f ./setup.sql
--
-- PREREQUISITE for CDC (not settable via SQL on RDS): logical replication
-- must be enabled in the DB parameter group — set rds.logical_replication = 1
-- (this makes wal_level = logical) and reboot the instance. Verify afterwards
-- with:  SHOW wal_level;   -- expect "logical"
--
-- Safe to re-run: role, database, grants, DDL and seed data are all idempotent.

\set ON_ERROR_STOP on

-- ---------------------------------------------------------------------------
-- 1. Roles (idempotent; passwords match the compose defaults — change these
--    for anything beyond a throwaway demo).
-- ---------------------------------------------------------------------------
DO $$
BEGIN
	IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'app_owner') THEN
		CREATE ROLE app_owner LOGIN PASSWORD 'app_owner';
	END IF;
	IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'debezium') THEN
		-- On RDS the REPLICATION attribute is superuser-only, so it is granted
		-- via the rds_replication role below instead of on the role itself.
		CREATE ROLE debezium LOGIN PASSWORD 'debezium';
	END IF;
END
$$;

-- Grant replication to debezium. On RDS this is done through rds_replication;
-- fall back to the ALTER ... REPLICATION attribute on a self-managed server.
DO $$
BEGIN
	IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'rds_replication') THEN
		GRANT rds_replication TO debezium;
	ELSE
		ALTER ROLE debezium REPLICATION;
	END IF;
END
$$;

-- Let the master user manage objects owned by app_owner (needed to SET ROLE
-- app_owner below so the app's user owns the schema and tables).
SELECT format('GRANT app_owner TO %I', current_user)\gexec

-- ---------------------------------------------------------------------------
-- 2. Database (CREATE DATABASE cannot run inside a transaction/DO block, so
--    guard it with \gexec).
-- ---------------------------------------------------------------------------
SELECT 'CREATE DATABASE ecommerce OWNER app_owner'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ecommerce')\gexec

GRANT CONNECT ON DATABASE ecommerce TO app_owner;
GRANT CONNECT ON DATABASE ecommerce TO debezium;

-- Switch into the target database for the rest of the script.
\connect ecommerce

-- ---------------------------------------------------------------------------
-- 3. Schema ownership & privileges (matches docker-compose.yaml).
-- ---------------------------------------------------------------------------
ALTER SCHEMA public OWNER TO app_owner;
GRANT USAGE, CREATE ON SCHEMA public TO app_owner;
GRANT USAGE ON SCHEMA public TO debezium;

-- Any table app_owner creates later is automatically SELECT-able by debezium.
ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA public
	GRANT SELECT ON TABLES TO debezium;
ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA public
	GRANT USAGE, SELECT ON SEQUENCES TO debezium;

-- ---------------------------------------------------------------------------
-- 4. Schema DDL + seed data, created AS app_owner so ownership and the
--    default-privilege SELECT grant to debezium both apply cleanly.
--    (Extracted from internal/database/postgres/ddl.go — FK order:
--    categories, products, users, orders, order_items.)
-- ---------------------------------------------------------------------------
SET ROLE app_owner;

CREATE TABLE IF NOT EXISTS categories (
	id           TEXT PRIMARY KEY,
	name         TEXT NOT NULL,
	description  TEXT,
	created_at   TIMESTAMPTZ NOT NULL,
	updated_at   TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
	id             TEXT PRIMARY KEY,
	name           TEXT NOT NULL,
	category_id    TEXT REFERENCES categories(id),
	price          NUMERIC(12,2) NOT NULL,
	stock_quantity INTEGER NOT NULL,
	tags           TEXT[],
	created_at     TIMESTAMPTZ NOT NULL,
	updated_at     TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
	id        TEXT PRIMARY KEY,
	username  TEXT NOT NULL,
	email     TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
	id           TEXT PRIMARY KEY,
	user_id      TEXT NOT NULL REFERENCES users(id),
	status       TEXT NOT NULL,
	total_amount NUMERIC(12,2) NOT NULL,
	created_at   TIMESTAMPTZ NOT NULL,
	updated_at   TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS order_items (
	order_id   TEXT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
	product_id TEXT NOT NULL,
	name       TEXT NOT NULL,
	quantity   INTEGER NOT NULL,
	unit_price NUMERIC(12,2) NOT NULL,
	PRIMARY KEY (order_id, product_id)
);

-- --- seed data (idempotent via ON CONFLICT) -------------------------------

INSERT INTO categories (id, name, description, created_at, updated_at) VALUES
	('cat-electronics', 'Electronics', 'Devices, gadgets and accessories', now(), now()),
	('cat-books',       'Books',       'Printed and digital books',        now(), now()),
	('cat-home',        'Home & Kitchen', 'Household and kitchen goods',    now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO products (id, name, category_id, price, stock_quantity, tags, created_at, updated_at) VALUES
	('prod-001', 'Wireless Headphones', 'cat-electronics', 129.99, 150, ARRAY['audio','wireless'], now(), now()),
	('prod-002', 'Mechanical Keyboard', 'cat-electronics',  89.50, 200, ARRAY['input','rgb'],      now(), now()),
	('prod-003', 'USB-C Charger 65W',   'cat-electronics',  34.00, 500, ARRAY['power'],            now(), now()),
	('prod-004', 'The Go Programming Language', 'cat-books', 42.75, 80, ARRAY['programming','go'], now(), now()),
	('prod-005', 'Designing Data-Intensive Applications', 'cat-books', 55.00, 60, ARRAY['data','systems'], now(), now()),
	('prod-006', 'Stainless Steel Kettle', 'cat-home',      49.99, 120, ARRAY['kitchen'],          now(), now()),
	('prod-007', 'Ceramic Mug Set',      'cat-home',        24.50, 300, ARRAY['kitchen','tableware'], now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, username, email) VALUES
	('user-001', 'alice',   'alice@example.com'),
	('user-002', 'bob',     'bob@example.com'),
	('user-003', 'carol',   'carol@example.com')
ON CONFLICT (id) DO NOTHING;

INSERT INTO orders (id, user_id, status, total_amount, created_at, updated_at) VALUES
	('order-001', 'user-001', 'completed',  219.49, now(), now()),
	('order-002', 'user-002', 'processing',  97.75, now(), now()),
	('order-003', 'user-003', 'pending',     49.99, now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_items (order_id, product_id, name, quantity, unit_price) VALUES
	('order-001', 'prod-001', 'Wireless Headphones', 1, 129.99),
	('order-001', 'prod-002', 'Mechanical Keyboard', 1,  89.50),
	('order-002', 'prod-004', 'The Go Programming Language', 1, 42.75),
	('order-002', 'prod-005', 'Designing Data-Intensive Applications', 1, 55.00),
	('order-003', 'prod-006', 'Stainless Steel Kettle', 1, 49.99)
ON CONFLICT (order_id, product_id) DO NOTHING;

RESET ROLE;

-- ---------------------------------------------------------------------------
-- 5. (Optional) Debezium publication for the pgoutput plugin. Create it up
--    front so the connector can run with publication.autocreate.mode=disabled.
--    Publications are owned by their creator; app_owner owns the tables, so
--    create it under that role.
-- ---------------------------------------------------------------------------
SET ROLE app_owner;
DO $$
BEGIN
	IF NOT EXISTS (SELECT FROM pg_publication WHERE pubname = 'dbz_ecommerce') THEN
		CREATE PUBLICATION dbz_ecommerce
			FOR TABLE categories, products, users, orders, order_items;
	END IF;
END
$$;
RESET ROLE;

-- Quick verification.
\echo 'Roles:'
\du app_owner
\du debezium
\echo 'Row counts:'
SELECT 'categories'  AS table, count(*) FROM categories
UNION ALL SELECT 'products',   count(*) FROM products
UNION ALL SELECT 'users',      count(*) FROM users
UNION ALL SELECT 'orders',     count(*) FROM orders
UNION ALL SELECT 'order_items', count(*) FROM order_items;
