/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.camel.pipeline;

import java.util.ArrayList;
import java.util.List;

public class Question {

    private long id;
    private String text;
    private String email;
    private final List<Answer> answers = new ArrayList<>();

    public Question() {
    }

    public Question(long id, String text, String email) {
        super();
        this.id = id;
        this.text = text;
        this.email = email;
    }

    public long getId() {
        return id;
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

    public void addOrUpdateAnswer(Answer answer) {
        int currentIndex = 0;
        int foundIndex = -1;

        for (Answer a: answers) {
            if (a.getId() == answer.getId()) {
                foundIndex = currentIndex;
                break;
            }
            currentIndex++;
        }
        if (foundIndex == -1) {
            answers.add(answer);
        }
        else {
            answers.set(foundIndex, answer);
        }
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    @Override
    public String toString() {
        return "Question [id=" + id + ", text=" + text + ", email=" + email + ", answers=" + answers + "]";
    }
}
