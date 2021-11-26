# Outbox Pattern with MongoDB

```shell
# Start the topology as defined in https://debezium.io/documentation/reference/stable/tutorial.html
export DEBEZIUM_VERSION=1.8
docker-compose up --build

# Initialize MongoDB replica set and insert some test data
docker-compose exec mongodb bash -c '/usr/local/bin/init-inventory.sh'

# Start MongoDB connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-mongodb.json

# Insert a new order and outbox event with multi-document transaction in the database via MongoDB client
docker-compose exec mongodb bash -c 'mongo -u $MONGODB_USER -p $MONGODB_PASSWORD --authenticationDatabase admin inventory'

new_order = { "_id" : ObjectId("000000000000000000000002"), "order_date" : ISODate("2021-11-22T00:00:00Z"), "purchaser_id" : NumberLong(1004), "quantity" : 1, "product_id" : NumberLong(107) }
s = db.getMongo().startSession()
s.startTransaction()
s.getDatabase("inventory").orders.insert(new_order)
s.getDatabase("inventory").outboxevent.insert({ _id : ObjectId("000000000000000000000002"), aggregateid : new_order._id, aggregatetype : "Order", type : "OrderCreated", timestamp: NumberLong(1556890294484), payload : new_order })
s.commitTransaction()

# Consume messages from the outbox event topic
docker run --tty --rm \
    --network mongo-outbox-network \
    debezium/tooling:1.2 \
    kafkacat -b kafka:9092 -C -o beginning -q \
    -t Order.events | jq .

# Shut down the cluster
docker-compose down
```