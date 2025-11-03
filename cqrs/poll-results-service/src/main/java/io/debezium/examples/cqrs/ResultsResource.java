package io.debezium.examples.cqrs;

import java.util.List;

import io.debezium.examples.cqrs.entity.OptionVotesEntity;
import io.debezium.examples.cqrs.entity.PollEntity;
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

    private static final String POLL_QUERY = "SELECT id, question FROM pollentity";
    private static final String VOTE_QUERY = """
            SELECT optionentity.polloption AS \"option\", count(voteentity.votedoption) AS votes  
            FROM voteentity JOIN optionentity ON voteentity.votedoption = optionentity.id 
            WHERE voteentity.pollid = :pollId 
            GROUP BY optionentity.polloption;
            """;

    @Inject
    private EntityManager em;

    @GET
    public List<PollEntity> getPolls() {
        try {
            Query q = em.createNativeQuery(POLL_QUERY, PollEntity.class);
            List<PollEntity> polls = q.getResultList();
            for (PollEntity poll : polls) {
                LOGGER.info("poll id: {}, question: {}", poll.id, poll.question);
            }
            return polls;
        }
        catch (Exception e) {
            if (e.getMessage().contains("table does not exist [table=pollentity]")) {
                LOGGER.warn("Table 'pollentity' does not exist, have you already deployed source and sink connectors?");
            }
            else {
                throw e;
            }
        }
        return null;
    }

    @GET
    @Path("{id}")
    public List<OptionVotesEntity> getResult(Long id) {
        try {
            Query q = em.createNativeQuery(VOTE_QUERY, OptionVotesEntity.class);
            q.setParameter("pollId", id);
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
