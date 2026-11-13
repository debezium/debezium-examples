/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphrag;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class Neo4jConnection {

    @ConfigProperty(name = "neo4j.uri")
    String uri;

    @ConfigProperty(name = "neo4j.username")
    String username;

    @ConfigProperty(name = "neo4j.password")
    String password;

    @Produces
    @ApplicationScoped
    Driver driver() {
        Log.debugf("Connecting to Neo4j at %s", uri);
        return GraphDatabase.driver(uri, AuthTokens.basic(username, password));
    }

    void close(@Disposes Driver driver) {
        driver.close();
    }
}
