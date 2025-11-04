package io.debezium.examples.cqrs;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import io.debezium.examples.cqrs.entity.PollEntity;
import io.debezium.examples.cqrs.entity.VoteEntity;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST endpoint for creating new poll and casting a vote in existing polls.
 */
@Path("poll")
@Produces("application/json")
@Consumes("application/json")
public class PollResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollResource.class);

    @GET
    public List<PollEntity> getAllPolls() {
        return PollEntity.listAll();
    }

    @GET
    @Path("{id}")
    public PollEntity getPoll(Long id) {
        PollEntity entity = PollEntity.findById(id);
        if (entity == null) {
            throw new WebApplicationException(String.format("Poll with id '%s' does not exist.", id), 404);
        }
        return entity;
    }

    @POST
    @Transactional
    public Response createPoll(PollEntity poll) {
        LOGGER.info("Creating poll {}", poll);
        if (poll.options.size() == 0) {
            throw new WebApplicationException("Invalid poll request.", 422);
        }
        poll.created = Timestamp.from(Instant.now());
        poll.persist();
        return Response.ok(poll).status(201).build();
    }

    @POST
    @Transactional
    @Path("{pollId}/vote/{optionId}")
    public Response castVote(Long pollId, Long optionId) {
        LOGGER.info("Voting for {} in poll {}", optionId, pollId);

        Optional<PollEntity> poll = PollEntity.findByIdOptional(pollId);
        if (poll.isEmpty()) {
            throw new WebApplicationException("Invalid vote request. Poll not found", 404);
        }
        if (poll.get().options.stream().noneMatch(o -> o.id.equals(optionId))) {
            throw new WebApplicationException("Invalid vote request. No such option", 404);
        }

        VoteEntity vote = new VoteEntity();
        vote.pollId = pollId;
        vote.votedOption = optionId;
        vote.votedOn = Timestamp.from(Instant.now());

        poll.get().votes.add(vote);
        poll.get().persist();

        return Response.ok(poll).status(201).build();
    }
}
