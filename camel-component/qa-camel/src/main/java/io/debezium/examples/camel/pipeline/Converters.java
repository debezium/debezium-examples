/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.camel.pipeline;

import org.apache.camel.Converter;
import org.apache.kafka.connect.data.Struct;

@Converter
public class Converters {

    @Converter
    public static Question questionFromStruct(Struct struct) {
        return new Question(struct.getInt64("id"), struct.getString("text"), struct.getString("email"));
    }

    @Converter
    public static Answer answerFromStruct(Struct struct) {
        return new Answer(struct.getInt64("id"), struct.getString("text"), struct.getString("email"), struct.getInt64("question_id"));
    }
}
