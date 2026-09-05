package org.acme;

import java.util.HashMap;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.hibernate.Session;
import org.hibernate.stat.Statistics;

@Path("/items")
@Produces(MediaType.APPLICATION_JSON)
public class ItemResource {

    @Inject
    EntityManager entityManager;

    @GET
    @Path("/{id}")
    public Response getItem(@PathParam("id") Long id) {
        Item item = entityManager.find(Item.class, id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        // Detach to ensure subsequent queries query L2 cache / DB rather than entity manager L1 context
        entityManager.detach(item);
        return Response.ok(item).build();
    }

    @GET
    @Path("/cache-stats")
    public Map<String, Object> getCacheStats() {
        Statistics stats = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        Map<String, Object> result = new HashMap<>();
        result.put("hitCount", stats.getSecondLevelCacheHitCount());
        result.put("missCount", stats.getSecondLevelCacheMissCount());
        result.put("putCount", stats.getSecondLevelCachePutCount());
        return result;
    }
}
