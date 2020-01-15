/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.camel.pipeline;

public class Answer {

    private long id;
    private String text;
    private String email;
    private long questionId;

    public Answer() {
    }

    public Answer(long id, String text, String email, long questionId) {
        super();
        this.id = id;
        this.text = text;
        this.email = email;
        this.questionId = questionId;
    }

    public long getId() {
        return id;
    }

    public long getQuestionId() {
        return questionId;
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

    @Override
    public String toString() {
        return "Answer [id=" + id + ", text=" + text + ", email=" + email + ", questionId=" + questionId + "]";
    }
}
