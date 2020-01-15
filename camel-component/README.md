First terminal
```
mvn clean install
mvn docker:start
```

Second terminal
```
java -jar qa-app/target/component-qa-app-1.0.0-SNAPSHOT-runner.jar
```

Third terminal
```
mvn exec:java -pl qa-camel
```

First terminal
```
curl -v -X POST -H 'Content-Type: application/json' http://0.0.0.0:8080/question/ -d @src/test/resources/messages/create-question.json
curl -v -X POST -H 'Content-Type: application/json' http://0.0.0.0:8080/question/1/answer -d @src/test/resources/messages/create-answer1.json
curl -v -X POST -H 'Content-Type: application/json' http://0.0.0.0:8080/question/1/answer -d @src/test/resources/messages/create-answer2.json
```

Open http://localhost:8025/ in browser - three emails should be visible
