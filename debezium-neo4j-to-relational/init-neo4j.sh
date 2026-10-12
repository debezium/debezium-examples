#!/usr/bin/env bash
#
# Enables Neo4j Change Data Capture on the `neo4j` database and populate the graph.
#
set -euo pipefail

NEO4J_ADDR="bolt://neo4j:7687"
NEO4J_USER="neo4j"
NEO4J_PASSWORD="password"

run() {
    cypher-shell -a "$NEO4J_ADDR" -u "$NEO4J_USER" -p "$NEO4J_PASSWORD" "$@"
}

echo "neo4j-init: waiting for Neo4j to accept connections..."
until run "RETURN 1;" >/dev/null 2>&1; do
    sleep 2
done

echo "neo4j-init: enabling CDC (txLogEnrichment=FULL) on database 'neo4j'..."
run -d system "ALTER DATABASE neo4j SET OPTION txLogEnrichment 'FULL';"

# SET OPTION restarts the database, so wait for it to come back online before seeding.
echo "neo4j-init: waiting for database to come back online..."
sleep 5
until run "RETURN 1;" >/dev/null 2>&1; do
    sleep 2
done

echo "neo4j-init: seeding graph..."
run -f /seed.cypher

echo "neo4j-init: graph seeded."
