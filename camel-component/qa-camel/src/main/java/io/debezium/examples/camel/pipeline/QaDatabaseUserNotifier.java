/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.camel.pipeline;

import java.time.Instant;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.debezium.DebeziumConstants;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.support.builder.PredicateBuilder;

import io.debezium.data.Envelope;

public class QaDatabaseUserNotifier extends RouteBuilder {
    private static final String SMTP_SERVER = "smtp://{{smtp.hostname}}:{{smtp.port}}?from=debezium-demo@localhost";
    private static final String ROUTE_MAIL_QUESTION_CREATE = "direct:mail-on-question-create";
    private static final String ROUTE_MAIL_ANSWER_CHANGE = "direct:mail-on-answer-change";
    private static final String TWITTER_SERVER =
                "twitter-timeline:user?"
                    + "consumerKey={{twitter.consumerKey}}"
                    + "&consumerSecret={{twitter.consumerSecret}}"
                    + "&accessToken={{twitter.accessToken}}"
                    + "&accessTokenSecret={{twitter.accessTokenSecret}}";

    static final String ROUTE_GET_AGGREGATE = "direct:get-aggregate";
    static final String ROUTE_WRITE_AGGREGATE = "direct:write-aggregate";

    private final String ROUTE_STORE_QUESTION_AGGREGATE = "infinispan://question";

    private static final String EVENT_TYPE_ANSWER = ".answer";
    private static final String EVENT_TYPE_QUESTION = ".question";

    @Override
    public void configure() throws Exception {
        final Predicate isCreateOrUpdateEvent =
                    header(DebeziumConstants.HEADER_OPERATION).in(
                            constant(Envelope.Operation.READ.code()),
                            constant(Envelope.Operation.CREATE.code()),
                            constant(Envelope.Operation.UPDATE.code()));

        final Predicate isCreateEvent =
                header(DebeziumConstants.HEADER_OPERATION).in(
                        constant(Envelope.Operation.READ.code()),
                        constant(Envelope.Operation.CREATE.code()));

        final Predicate isQuestionEvent =
                header(DebeziumConstants.HEADER_IDENTIFIER).endsWith(EVENT_TYPE_QUESTION);

        final Predicate isAnswerEvent =
                header(DebeziumConstants.HEADER_IDENTIFIER).endsWith(EVENT_TYPE_ANSWER);

        final Predicate hasManyAnswers =
                PredicateBuilder.and(
                        isCreateEvent,
                        simple("${exchangeProperty[aggregate].answers.size} == 3"));

        final AggregateStore store = new AggregateStore();

        from(ROUTE_MAIL_QUESTION_CREATE)
            .routeId(QaDatabaseUserNotifier.class.getName() + ".QuestionNotifier")
            .setHeader("To").simple("${body.email}")
            .setHeader("Subject").simple("Question created/edited")
            .setBody().simple("Question '${body.text}' was created or edited")
            .to(SMTP_SERVER);

        from(ROUTE_MAIL_ANSWER_CHANGE)
            .routeId(QaDatabaseUserNotifier.class.getName() + ".AnswerNotifier")
            .setHeader("To").simple("${body.email}; ${exchangeProperty[aggregate].email}")
            .setHeader("Subject").simple("Answer created/edited")
            .setBody().simple("Answer '${body.text}' was added/updated for question '${exchangeProperty[aggregate].text}'")
            .to(SMTP_SERVER);

        from(ROUTE_GET_AGGREGATE)
            .routeId(QaDatabaseUserNotifier.class.getName() + ".ReadAggregate")
            .setHeader(InfinispanConstants.KEY).body()
            .setHeader(InfinispanConstants.OPERATION).constant("GET")
            .to(ROUTE_STORE_QUESTION_AGGREGATE)
            .filter(body().isNotNull())
                .unmarshal().json(JsonLibrary.Jackson, Question.class)
                .log(LoggingLevel.TRACE, "Unarshalled question ${body}");

        from(ROUTE_WRITE_AGGREGATE)
            .routeId(QaDatabaseUserNotifier.class.getName() + ".WriteAggregate")
            .setHeader(InfinispanConstants.KEY).simple("${body.id}")
            .log(LoggingLevel.TRACE, "About to marshall ${body}")
            .marshal().json(JsonLibrary.Jackson)
            .log(LoggingLevel.TRACE, "Marshalled question ${body}")
            .setHeader(InfinispanConstants.VALUE).body()
            .to(ROUTE_STORE_QUESTION_AGGREGATE);

        from("debezium-postgres:localhost?"
                + "databaseHostname={{database.hostname}}"
                + "&databasePort={{database.port}}"
                + "&databaseUser={{database.user}}"
                + "&databasePassword={{database.password}}"
                + "&databaseDbname=postgres"
                + "&topicPrefix=qa"
                + "&schemaWhitelist={{database.schema}}"
                + "&tableWhitelist={{database.schema}}.question,{{database.schema}}.answer"
                + "&offsetStorage=org.apache.kafka.connect.storage.MemoryOffsetBackingStore")
                .routeId(QaDatabaseUserNotifier.class.getName() + ".DatabaseReader")
                .log(LoggingLevel.DEBUG, "Incoming message ${body} with headers ${headers}")
                .choice()
                    .when(isQuestionEvent)
                        .filter(isCreateOrUpdateEvent)
                            .convertBodyTo(Question.class)
                            .log(LoggingLevel.TRACE, "Converted to logical class ${body}")
                            .bean(store, "readFromStoreAndUpdateIfNeeded")
                            .to(ROUTE_MAIL_QUESTION_CREATE)
                        .endChoice()
                    .when(isAnswerEvent)
                        .filter(isCreateOrUpdateEvent)
                            .convertBodyTo(Answer.class)
                            .log(LoggingLevel.TRACE, "Converted to logical class ${body}")
                            .bean(store, "readFromStoreAndAddAnswer")
                            .to(ROUTE_MAIL_ANSWER_CHANGE)
                            .filter(hasManyAnswers)
                                .setBody().simple("Question '${exchangeProperty[aggregate].text}' has many answers (generated at " + Instant.now() + ")")
                                .to(TWITTER_SERVER)
                            .end()
                        .endChoice()
                    .otherwise()
                        .log(LoggingLevel.WARN, "Unknown type ${headers[" + DebeziumConstants.HEADER_IDENTIFIER + "]}")
                .endParent();
    }
}
