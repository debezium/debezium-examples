/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.cacheinvalidation;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.restassured.http.ContentType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CacheInvalidationTest {

    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String jdbcUrl;

    @ConfigProperty(name = "quarkus.datasource.username")
    String jdbcUserName;

    @ConfigProperty(name = "quarkus.datasource.password")
    String jdbcPassword;

    @BeforeEach
    public void prepareItem() {
        updateItem(10003, "North by Northwest", 14.99F);
    }

    @Test
    public void shouldInvalidateCacheAfterDatabaseUpdate() throws Exception {
        placeOrder(10003, 2, 29.98F);

        // update the item price directly in the DB
        try(Connection conn = getDbConnection(); Statement statement = conn.createStatement()) {
            statement.executeUpdate("UPDATE public.item SET price = 16.99 WHERE id = 10003;");
        }

        // cache should be invalidated
        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> {
                return !get("/rest/cache/item/10003").as(boolean.class);
            });

        // and the item reloaded from the DB
        placeOrder(10003, 2, 33.98F);
    }

    @Test
    public void shouldNotInvalidateCacheAfterUpdateThroughApplication() throws Exception {
        placeOrder(10003, 2, 29.98F);

        // update the item price through application
        updateItem(10003, "North by Northwest", 16.99F);

        // Theoretically an (unexpected) CDC event could also arrive after that time,
        // but that seems to be as good as it gets
        Thread.sleep(3000);

        // cache should not be invalidated
        assertTrue(get("/rest/cache/item/10003").as(boolean.class));
    }

    private void placeOrder(long itemId, int quantity, float expectedTotalPrice) {
        given()
            .contentType(ContentType.JSON)
            .body(
                "{\n" +
                "    \"customer\" : \"Billy-Bob\",\n" +
                "    \"itemId\" : " + itemId + ",\n" +
                "    \"quantity\" : " + quantity + "\n" +
                "}"
            )
        .when()
            .post("/rest/orders")
        .then()
            .body("totalPrice", equalTo(expectedTotalPrice));
    }

    private void updateItem(long itemId, String newDescription, float newPrice) {
        given()
            .contentType(ContentType.JSON)
            .body(
                "{\n" +
                "    \"description\" : \"" + newDescription + "\",\n" +
                "    \"price\" : " + newPrice + "\n" +
                "}"
            )
        .when()
            .put("/rest/items/{id}", itemId)
        .then()
            .statusCode(200);
    }

    private Connection getDbConnection() throws SQLException {
        final Properties props = new Properties();
        props.setProperty("user", jdbcUserName);
        props.setProperty("password", jdbcPassword);

        return DriverManager.getConnection(jdbcUrl, props);
    }
}
