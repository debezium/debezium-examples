-- generate_products.sql — append N synthetic "Cat Home" products to the ecommerce demo.
--
-- Companion to setup.sql. Run it (repeatedly) to generate CDC traffic on the
-- products table: each run inserts up to 500 new rows with sequential ids
-- (prod-cat-home-0001, prod-cat-home-0002, ...), continuing from the highest
-- existing number, so it is safe to re-run.
--
--   psql -h localhost -p 5432 -U <master-user> -d ecommerce -f ./generate_products.sql

WITH next_id AS (
    SELECT COALESCE(
        MAX(substring(id FROM 'prod-cat-home-(\d+)$')::integer),
        0
    ) AS last_num
    FROM products
    WHERE id ~ '^prod-cat-home-\d+$'
)
INSERT INTO products (
    id,
    name,
    category_id,
    price,
    stock_quantity,
    tags,
    created_at,
    updated_at
)
SELECT
    'prod-cat-home-' || lpad((next_id.last_num + gs)::text, 4, '0') AS id,
    'Cat Home Product ' || (next_id.last_num + gs) AS name,
    'cat-home' AS category_id,
    round((random() * 200 + 5)::numeric, 2) AS price,
    floor(random() * 500)::integer AS stock_quantity,
    ARRAY[
        'cat',
        'home',
        CASE
            WHEN gs % 5 = 0 THEN 'premium'
            WHEN gs % 5 = 1 THEN 'cozy'
            WHEN gs % 5 = 2 THEN 'durable'
            WHEN gs % 5 = 3 THEN 'modern'
            ELSE 'essential'
        END
    ] AS tags,
    now() - (random() * interval '365 days') AS created_at,
    now() AS updated_at
FROM next_id
CROSS JOIN generate_series(1, 500) AS gs
ON CONFLICT (id) DO NOTHING;
