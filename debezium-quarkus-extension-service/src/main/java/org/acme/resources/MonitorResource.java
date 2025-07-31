package org.acme.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.SnapshotService;

@Path("/monitor")
public class MonitorResource {

    private final SnapshotService snapshotService;

    @Inject
    public MonitorResource(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @GET()
    @Path("/snapshot")
    public Response get() {
        return Response.ok(snapshotService.getLast()).build();
    }
}
