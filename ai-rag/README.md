# Retrieval Augmented Generation wit Debezium

This demo shows how Debezium Server could be used to implement RAG in an application.

## Use case

Application downloads papers from [arXiv](https://arxiv.org/) that shares scholar articles.
The downloaded paper is stored in PostgreSQL database.
Update to the database is captured by Debezium Server.
Debezium Server will calculate embedding out of the downloaded paper using [FieldToEmbedding SMT](https://debezium.io/blog/2025/04/02/debezium-3-1-final-released/#new-features-and-improvements-ai) and store it in Milvus vector database.
The `ollama` serverd LLM is started locally and the client application acts and interface to it while providing additional knowledge in prompt context obtained from Milvus database.

## Steps to run demo

For testing we will use paper named [IterQR: An Iterative Framework for LLM-based Query Rewrite in e-Commercial Search System](https://arxiv.org/abs/2504.05309) as `IterQR` word is not know by the model.
The demo will run in three terminals

### Deployment start (Terminal 1)

Execute
```
$ docker-compose up --build`
```

This will start up PostgreSQL, Milvus and ollama.
Debezium Server image is extended with `debezium-ai-embeddings-ollama` module and started.

### Start LLM (Terminal 2)

Execute
```
$ docker exec -it ollama ollama run granite3.1-dense:2b
```

This will start Granite LLM and provide CLI to interface with.

Type prompt to LLM

```
Describe IterQR framework in 20 words
```

The result should be a hallucinated reply completely unrelated to the real meaning of `IterQR`.
The reply will be different each time, one sample reply can be

```
>>> Describe IterQR framework in 20 words.
Iterative Quantized Ridge Regression (IterQR) is an efficient algorithm for large-scale linear regression 
that quantizes weights during iterations to reduce memory and computational demands, improving scalability.
```

### RAG application (Terminal 3)

There is a Quarkus based application available that uses RAG to extend LLM knowledge with new facts.

Build application

```
$ cd client
$ ./mvnw clean install
```

The CLI is executed via `java -jar target/quarkus-app/quarkus-run.jar` command.

Create and initialize Milvus collection used by the application

```
$ java -jar target/quarkus-app/quarkus-run.jar init

...
2025-05-19 10:01:51,875 INFO  [io.deb.exa.air.MilvusStore] (main) Created collection 'demo_ai_documents' with schema:
...
```

There is one (dummy) document stored in the collection

```
$ java -jar target/quarkus-app/quarkus-run.jar list-milvus

...
2025-05-19 10:04:04,323 INFO  [io.deb.exa.air.MilvusStore] (main) Milvus list results size: 1
2025-05-19 10:04:04,326 INFO  [io.deb.exa.air.MilvusStore] (main) Document: 
...
```

Ask about `IterQR`.
The knowledge is not available yet so there should be a hallucinated reply.

```
$ java -jar target/quarkus-app/quarkus-run.jar query-chat Describe IterQR framework in 20 words

...
2025-05-19 10:06:39,373 INFO  [io.deb.exa.air.RagCommand] (main) Chat reply: Iterative QR, or iterated Quadratic Regression, is a numerical method for solving large, sparse linear systems, optimizing permanently through multiple iterations. It combines QR decomposition and preconditioning techniques.
...
```

Add new knowledge: document is download and inserted into PostgreSQL table.
Verify that there is a new record available in Milvus collection that was provided by Debezium Server.

```
$ java -jar target/quarkus-app/quarkus-run.jar insert-document 2504.05309v1

$ java -jar target/quarkus-app/quarkus-run.jar list-milvus

...
2025-05-19 10:10:55,512 INFO  [io.deb.exa.air.MilvusStore] (main) Milvus list results size: 2
...
```

Ask about `IterQR`.
The knowledge is available so it should be passed as a context in the prompt and the reply should reflect it.

```
$ java -jar target/quarkus-app/quarkus-run.jar query-chat Describe IterQR framework in 20 words

...
2025-05-19 10:13:06,996 INFO  [io.deb.exa.air.RagCommand] (main) Chat reply: IterQR framework is a methodology for enhancing e-Commercial search systems, optimizing intent matching and candidate retrieval via iterative query rewriting, mitigating user input errors.
...
```

Remove the document from the PostgreSQL table.
This will remove it from Milvus sink too and will become lost/forgotten knowledge.
With the repeated question we will again receive hallucinated reply.

```
$ java -jar target/quarkus-app/quarkus-run.jar delete-document 2504.05309v1

$ java -jar target/quarkus-app/quarkus-run.jar list-milvus

...
2025-05-19 10:17:33,712 INFO  [io.deb.exa.air.MilvusStore] (main) Milvus list results size: 1
...

$ java -jar target/quarkus-app/quarkus-run.jar query-chat Describe IterQR framework in 20 words

...
2025-05-19 10:18:37,575 INFO  [io.deb.exa.air.RagCommand] (main) Chat reply: Iterative QR, or Iterated Q-function Approximation for Reinforcement Learning, is an algorithm that iteratively refines Q-values by approximating them with a linear function. It's used to solve Markov Decision Processes via deep learning methods.
...
```

### Stop demo

```
docker-compose down
```