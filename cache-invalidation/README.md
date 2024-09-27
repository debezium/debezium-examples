# JPA Cache Invalidation

This demo shows how Debezium can be used to invalidate items in the JPA 2nd level cache after external data changes,
e.g. a manual record update in the database, bypassing the application layer.

The application uses Quarkus, Hibernate, and PostgreSQL as a database.
The domain model is centered around purchase orders of given items.
The `Item` entity is marked as cacheable, i.e. after updates to an item (e.g. its base price),
it must be purged from the 2nd-level cache in order to correctly calculate the price of future orders of that item.

## Manual Testing

1. First start the database container.
   ```bash
   mvn docker:start
   ```
   This will start the `quay.io/debezium/example-postgres:latest` container that will be used to store our JPA entities and the database that the Debezium will capture changes from, too.

2. Run the application using the Quarkus development mode.
   ```bash
   mvn clean quarkus:dev
   ``` 
   The application will start in the development mode and run until you press `Ctrl+C` to stop the application.

3. Place an order for item 1003 using curl:
   ```bash
   curl -H "Content-Type: application/json" \
      -X POST \
      --data @resources/data/create-order-request.json \
      http://localhost:8080/rest/orders   
   ```
   Or, if [httpie](https://httpie.org/) is your preferred CLI HTTP client:
   ```bash
   cat resources/data/create-order-request.json | http POST http://localhost:8080/rest/orders
   ```

4. Update the price of item 10003 directly in the database:
   ```bash
   docker exec postgres-server-test-database-1 bash -c 'psql -U $POSTGRES_USER $POSTGRES_DB -c "UPDATE item SET price = 20.99 where id = 10003"'
   ```

5. Now use the REST endpoint to verify that the item has been purged from the cache:
   ```bash
   curl -H "Content-Type: application/json" \
      -X GET \
      http://localhost:8080/rest/cache/item/10003
   ```
   or via httpie:
   ```bash
   http GET http://localhost:8080/rest/cache/item/10003
   ```

6. Place another order of that item and observe how the calculated total price reflects the change applied above.
   Also observe the application's log how the `item` table is queried.

7. Now, update the item again using the application's REST endpoint this time:
   ```bash
   curl -H "Content-Type: application/json" \
      -X PUT \
      --data @resources/data/update-item-request.json \
      http://localhost:8080/rest/items/10003
   ```
   or via httpie:
   ```bash
   cat resources/data/update-item-request.json | http PUT http://localhost:8080/rest/items/10003
   ```
   
8. The Debezium CDC event handler detects this transaction is issued by the application, which results in the item not being removed from the cache:
   You can test this use case using curl:
   ```bash
   curl -H "Content-Type: application/json" \
      -X GET \
      http://localhost:8080/rest/cache/item/10003
   ```
   or using httpie:
   ```bash
   http GET http://localhost:8080/rest/cache/item/10003
   ```

9. If you place another order, the `Item` entity is obtained from the cache, avoiding the database round-trip.

10. Press `Ctrl+C` in the terminal to stop the Quarkus running application.
    
11. Execute `mvn docker:stop`, to stop the PostgreSQL database container.


## Development

### Start-up steps:
1. 
2. First start the PostgreSQL database container using maven:
   ```bash
   mvn docker:start
   ```
   
2. Next start the application in Quarkus development mode:
   ```bash
   mvn clean quarkus:dev
   ```
   
### Accessing the database

To obtain a database session in PostgreSQL, run:
```bash
docker run -it --rm --link postgres-1:postgres quay.io/debezium/example-postgres:latest psql -h postgres -U postgresuser --dbname inventory
```

### Shutdown steps

1. Stop the Quarkus application by pressing `Ctrl+C` in the terminal.
2. Execute `mvn docker:stop` to stop the PostgreSQL database container.
