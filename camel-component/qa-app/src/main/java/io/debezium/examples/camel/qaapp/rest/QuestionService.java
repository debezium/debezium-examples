/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.camel.qaapp.rest;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.debezium.examples.camel.qaapp.Answer;
import io.debezium.examples.camel.qaapp.Question;

@Path("/question")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QuestionService {

    @Inject
    private EntityManager em;

    @POST
    @Transactional
    public void addQuestion(Question question) {
        em.merge(question);
    }

    @POST
    @Transactional
    @Path("{questionId}/answer")
    public void addAnswer(@PathParam("questionId") long questionId, Answer answer) {
        final Question question = em.find(Question.class, questionId);
        answer.setQuestion(question);
        question.addAnswer(answer);
    }
}
