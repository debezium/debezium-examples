#!/bin/sh

http PUT http://localhost:8083/connectors/order-outbox-connector/config < register-order-connector.json
http PUT http://localhost:8083/connectors/payment-outbox-connector/config < register-payment-connector.json
http PUT http://localhost:8083/connectors/credit-outbox-connector/config < register-credit-connector.json
http PUT http://localhost:8083/connectors/order-sagastate-connector/config < register-sagastate-connector.json
