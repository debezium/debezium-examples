package io.debezium.demos.auditing.vegetables.rest.util;

import javax.json.Json;
import javax.persistence.EntityNotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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
