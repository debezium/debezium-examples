#!/bin/sh

http DELETE http://localhost:8083/connectors/order-outbox-connector
http DELETE http://localhost:8083/connectors/payment-outbox-connector
http DELETE http://localhost:8083/connectors/credit-outbox-connector
http DELETE http://localhost:8083/connectors/order-sagastate-connector
