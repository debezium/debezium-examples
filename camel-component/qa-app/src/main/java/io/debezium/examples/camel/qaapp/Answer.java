/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.camel.qaapp;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import io.debezium.examples.camel.qaapp.Vote.VoteType;

@Entity
public class Answer {

    @Id
    @GeneratedValue
    private long id;

    private String email;
    private String text;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id", updatable = false, nullable = false)
    private Question question;

    private transient List<Vote> votes = new ArrayList<>();

    public Answer() {
    }

    public Answer(String email, String text) {
        this.email = email;
        this.text = text;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public void upVote(String email) {
        votes.add(new Vote(email, VoteType.UP));
    }

    public void downVote(String email) {
        votes.add(new Vote(email, VoteType.DOWN));
    }
}
