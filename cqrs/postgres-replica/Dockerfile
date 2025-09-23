FROM mirror.gcr.io/library/postgres:17-bookworm

COPY postgresql.conf.sample /usr/local/share/postgresql/postgresql.conf.sample
COPY /docker-entrypoint-initdb.d /docker-entrypoint-initdb.d
