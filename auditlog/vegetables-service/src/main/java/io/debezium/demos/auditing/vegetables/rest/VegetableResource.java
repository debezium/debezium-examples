package io.debezium.demos.auditing.vegetables.rest;

import java.time.ZonedDateTime;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.debezium.demos.auditing.vegetables.model.Vegetable;
import io.debezium.demos.auditing.vegetables.service.VegetableService;
import io.debezium.demos.auditing.vegetables.transactioncontext.TransactionContext;

@Path("/vegetables")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VegetableResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    RequestContext requestContext;

    @Inject
    VegetableService vegetableService;

    @POST
    @RolesAllowed({"farmers"})
    @Transactional
    @TransactionContext(useCase="CREATE VEGETABLE")
    public Response createVegetable(Vegetable vegetable, @HeaderParam(HttpHeaders.DATE) ZonedDateTime date) {
        if (vegetable.getId() != null) {
            return Response.status(Status.BAD_REQUEST.getStatusCode()).build();
        }

        updateRequestContext(date);

        vegetable = vegetableService.createVegetable(vegetable);

        return Response.ok(vegetable).status(Status.CREATED).build();
    }

    @Path("/{id}")
    @PUT
    @RolesAllowed({"farmers"})
    @Transactional
    @TransactionContext(useCase="UPDATE VEGETABLE")
    public Vegetable updateVegetable(@PathParam("id") long id, Vegetable vegetable, @HeaderParam(HttpHeaders.DATE) ZonedDateTime date) {
        updateRequestContext(date);

        vegetable.setId(id);
        vegetable = vegetableService.updateVegetable(vegetable);

        return vegetable;
    }

    @Path("/{id}")
    @DELETE
    @RolesAllowed({"farmers"})
    @Transactional
    @TransactionContext(useCase="DELETE VEGETABLE")
    public void deleteVegetable(@PathParam("id") long id, @HeaderParam(HttpHeaders.DATE) ZonedDateTime date) {
        updateRequestContext(date);

        vegetableService.deleteVegetable(id);
    }

    private void updateRequestContext(ZonedDateTime date) {
        requestContext.userName = jwt.<String>claim("sub").orElse("anonymous");
        requestContext.date = date;
    }
}
