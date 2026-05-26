package io.debezium.demos.auditing.vegetables.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.debezium.demos.auditing.vegetables.model.Vegetable;
import io.debezium.demos.auditing.vegetables.service.VegetableService;
import io.debezium.demos.auditing.vegetables.transactioncontext.Audited;

@Path("/vegetables")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VegetableResource {

    @Inject
    VegetableService vegetableService;

    @POST
    @RolesAllowed({"farmers"})
    @Transactional
    @Audited(useCase="CREATE VEGETABLE")
    public Response createVegetable(Vegetable vegetable) {
        if (vegetable.getId() != null) {
            return Response.status(Status.BAD_REQUEST.getStatusCode()).build();
        }

        vegetable = vegetableService.createVegetable(vegetable);

        return Response.ok(vegetable).status(Status.CREATED).build();
    }

    @Path("/{id}")
    @PUT
    @RolesAllowed({"farmers"})
    @Transactional
    @Audited(useCase="UPDATE VEGETABLE")
    public Vegetable updateVegetable(@PathParam("id") long id, Vegetable vegetable) {
        vegetable.setId(id);
        vegetable = vegetableService.updateVegetable(vegetable);

        return vegetable;
    }

    @Path("/{id}")
    @DELETE
    @RolesAllowed({"farmers"})
    @Transactional
    @Audited(useCase="DELETE VEGETABLE")
    public void deleteVegetable(@PathParam("id") long id) {
        vegetableService.deleteVegetable(id);
    }
}
