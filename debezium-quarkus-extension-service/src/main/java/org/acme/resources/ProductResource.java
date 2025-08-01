package org.acme.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.ProductsService;

@Path("/products")
public class ProductResource {

    private final ProductsService productsService;

    @Inject
    public ProductResource(ProductsService productsService) {
        this.productsService = productsService;
    }

    @GET
    public Response get() {
        return Response.ok(productsService.get()).build();
    }
}
