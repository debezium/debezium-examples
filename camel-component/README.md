# Apache Camel component for Debezium

The integration framework [Apache Camel](https://camel.apache.org/) provides a set of components integrating Debezium with Apache Camel.
This example demonstrate a sample application and an integration pipeline capturing changes in the database generate by the application.

## Topology
The example consists of multiple components

* application accepting REST calls representing questions and answers to the questions
* PostgreSQL database to which the application writes the questions and answers
* MailHog test SMTP server
* Apache Camel pipeline that
  * captures database changes
  * converts the raw changes into domain Java classes
  * creates an aggregate object from answers and associated question using Infinispan-based message store
  * sends an email to author for every question created
  * sends an email is sent to author of question and author of the answer for every answer created
  * when a question has three answers a message is send to a Twitter timeline

## How to run

Build the application and the pipeline and start PostgreSQL instance and MailHog server

```
# Terminal One
$ mvn clean install
$ mvn docker:start
```

Start the Q&A application
```
# Terminal Two
$ java -jar qa-app/target/component-qa-app-1.0.0-SNAPSHOT-runner.jar
```

Start the integration pipeline (you need a Twitter developer account)
```
# Terminal Three
$ mvn exec:java -Dtwitter.consumerKey=<...> -Dtwitter.consumerSecret=<...> -Dtwitter.accessToken=<...> -Dtwitter.accessTokenSecret=<...> -pl qa-camel
```

Create a question and three answers to it
```
# Terminal One
$ curl -v -X POST -H 'Content-Type: application/json' http://0.0.0.0:8080/question/ -d @src/test/resources/messages/create-question.json
$ curl -v -X POST -H 'Content-Type: application/json' http://0.0.0.0:8080/question/1/answer -d @src/test/resources/messages/create-answer1.json
$ curl -v -X POST -H 'Content-Type: application/json' http://0.0.0.0:8080/question/1/answer -d @src/test/resources/messages/create-answer2.json
$ curl -v -X POST -H 'Content-Type: application/json' http://0.0.0.0:8080/question/1/answer -d @src/test/resources/messages/create-answer3.json
```

Check the emails sent in MailHog UI (http://localhost:8025/) in browser - four emails should be visible and your Twitter account should containe a message like `Question 'How many legs does a dog have?' has many answers (...)`.
