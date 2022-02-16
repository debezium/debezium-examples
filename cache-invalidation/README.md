# JPA Cache Invalidation

This demo shows how Debezium can be used to invalidate items in the JPA 2nd level cache after external data changes,
e.g. a manual record update in the database, bypassing the application layer.

The application runs on WildFly and uses Postgres as a database.
The domain model is centered around purchase orders of given items.
The `Item` entity is marked as cacheable, i.e. after updates to an item (e.g. its base price),
it must be purged from the 2nd-level cache in order to correctly calculate the price of future orders of that item.

## Manual Testing

To run the app, follow these steps:

    export DEBEZIUM_VERSION=1.8
    mvn clean package
    docker-compose up --build

Place an order for item 10003 using curl:

    curl -H "Content-Type: application/json" \
      -X POST \
      --data @resources/data/create-order-request.json \
      http://localhost:8080/cache-invalidation/rest/orders

Or, if [httpie](https://httpie.org/) is your preferred CLI HTTP client:

    cat resources/data/create-order-request.json | http POST http://localhost:8080/cache-invalidation/rest/orders

Update the price of item 10003 directly in the database:

    docker-compose exec postgres bash -c 'psql -U $POSTGRES_USER $POSTGRES_DB -c "UPDATE item SET price = 20.99 where id = 10003"'

Use the application's REST API to verify that the item has been purged from the cache:

    curl -H "Content-Type: application/json" \
      -X GET \
      http://localhost:8080/cache-invalidation/rest/cache/item/10003

Or via httpie:

    http GET http://localhost:8080/cache-invalidation/rest/cache/item/10003

Place another order of that item and observe how the calculated total price reflects the change applied above.
Also observe in the application's log how the `item` table is queried.

Now update the item again, using the application's REST API this time:

    curl -H "Content-Type: application/json" \
      -X PUT \
      --data @resources/data/update-item-request.json \
      http://localhost:8080/cache-invalidation/rest/items/10003

Or via httpie:

    cat resources/data/update-item-request.json | http PUT http://localhost:8080/cache-invalidation/rest/items/10003

The Debezium event handler will detect that this transaction is issued by the application itself, resulting in the item to not be removed from the cache:

    curl -H "Content-Type: application/json" \
      -X GET \
      http://localhost:8080/cache-invalidation/rest/cache/item/10003

Or via httpie:

    http GET http://localhost:8080/cache-invalidation/rest/cache/item/10003

If you place yet another order, you'll see how the `Item` entity is obtained from the cache, avoiding the roundtrip to the database.

Finally, shut down database and application server:

    docker-compose down

## Build

Run

    mvn clean package

This will build the application, deploy it to WildFly via Docker and run an integration test against it.

## Development

During development, start up database and WildFly like so:

    mvn docker:build docker:start

After code changes the application can be re-deployed like so:

    mvn wildfly:redeploy

To get a session in Postgres run:

    docker run -it --rm --link postgres-1:postgres debezium/example-postgres:${DEBEZIUM_VERSION} psql -h postgres -U postgresuser --dbname inventory

Run

    mvn docker:stop

to shut down all the started containers.
