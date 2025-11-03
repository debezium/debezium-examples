package io.debezium.examples.cqrs;

import java.util.List;

import io.debezium.examples.cqrs.entity.OptionVotesEntity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST endpoint serving selected poll results.
 *
 * @author vjuranek
 */
@Path("results")
@Produces("application/json")
@Consumes("application/json")
public class ResultsResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsResource.class);

    @ConfigProperty(name="results.query")
    private String resultQuery;

    @Inject
    private EntityManager em;

    @GET
    @Path("{id}")
    public List<OptionVotesEntity> getResult(Long id) {
        try {
            Query q = em.createNativeQuery(resultQuery, OptionVotesEntity.class);
            List<OptionVotesEntity> res = q.getResultList();
            for (OptionVotesEntity o : res) {
                LOGGER.info("result: {}, votes: {}", o.option, o.votes);
            }
            return res;
        }
        catch (Exception e) {
            if (e.getMessage().contains("table does not exist [table=votes]")) {
                LOGGER.warn("Table 'votes' does not exist, have you already deployed source and sink connectors?");
            }
            else {
                throw e;
            }
        }
        return null;
    }
}
