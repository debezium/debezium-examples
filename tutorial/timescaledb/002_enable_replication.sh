#!/bin/bash

echo "wal_level=logical" >> ${POSTGRESQL_CONF_DIR}/postgresql.conf

psql -U "${POSTGRES_USER}" "${POSTGRES_DB}" -c "CREATE TABLE conditions (time TIMESTAMPTZ NOT NULL, location TEXT NOT NULL, temperature DOUBLE PRECISION NULL, humidity DOUBLE PRECISION NULL); SELECT create_hypertable('conditions', 'time'); INSERT INTO conditions VALUES(NOW(), 'Prague', 22.8,  53.3); CREATE PUBLICATION dbz_publication FOR ALL TABLES WITH (publish = 'insert, update')"
