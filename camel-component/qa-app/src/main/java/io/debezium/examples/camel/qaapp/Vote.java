/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.camel.qaapp;

public class Vote {

    public static enum VoteType {
        UP,
        DOWN
    }

    private final String email;
    private final VoteType voteType;

    public Vote(String email, VoteType voteType) {
        this.email = email;
        this.voteType = voteType;
    }

    public int voteValue() {
        return voteType == VoteType.UP ? 1 : -1;
    }
}

