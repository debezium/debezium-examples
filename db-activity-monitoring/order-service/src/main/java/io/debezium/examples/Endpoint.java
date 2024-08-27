package io.debezium.examples;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;

import java.nio.file.Paths;

@Path("")
public class Endpoint {

    @GET
    @Path("hello")
    public RestResponse<java.nio.file.Path> hello() {
        // HTTP OK status with text/plain content type

        java.nio.file.Path file = Paths.get("inventory.sql");
        return RestResponse.ResponseBuilder.ok(file, MediaType.APPLICATION_OCTET_STREAM_TYPE)
                // set a response header
                .header("Content-Disposition", "attachment; filename="
                        + file.getFileName())
                // end of builder API
                .build();
    }
}