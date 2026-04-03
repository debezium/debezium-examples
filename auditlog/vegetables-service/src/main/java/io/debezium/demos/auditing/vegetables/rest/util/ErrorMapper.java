package io.debezium.demos.auditing.vegetables.rest.util;

import jakarta.json.Json;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ErrorMapper implements ExceptionMapper<EntityNotFoundException> {

    @Override
    public Response toResponse(EntityNotFoundException exception) {
        return Response.status(Status.NOT_FOUND)
                .entity(Json.createObjectBuilder()
                        .add("error", exception.getMessage())
                        .build())
                .build();
    }
}
