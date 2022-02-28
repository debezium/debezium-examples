ARG DEBEZIUM_VERSION
FROM debezium/example-mysql:${DEBEZIUM_VERSION}

COPY schema-update.sql /docker-entrypoint-initdb.d/
