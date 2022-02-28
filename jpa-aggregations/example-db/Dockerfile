ARG DEBEZIUM_VERSION
FROM quay.io/debezium/example-mysql:${DEBEZIUM_VERSION}

COPY schema-update.sql /docker-entrypoint-initdb.d/
