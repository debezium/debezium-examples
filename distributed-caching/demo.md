# Demo Commands

* Change replica identity to full:

```
docker run --tty --rm -i \
    --network distributed-caching-network \
    quay.io/debezium/tooling:1.2 \
    bash -c 'pgcli postgresql://postgresuser:postgrespw@order-db:5432/orderdb'

ALTER TABLE inventory.purchaseorder REPLICA IDENTITY FULL;
ALTER TABLE inventory.orderline REPLICA IDENTITY FULL;
```

* Select orders with cancelled items from the cache:

```
from caching.PurchaseOrder po where po.lineItems.status="CANCELLED"
```
