Steps for demo

Will test on new knowledge
* https://arxiv.org/abs/2504.05309
* Selected because of IterQR word, not present before model cutoff

* Terminal 1
    * `docker-compose up`
    * Wait for components to start
* Terminal 2
    * Start Granite model via `docker exec -it ollama ollama run granite3.1-dense:2b`
    * Test using prompt `Describe IterQR framework in 20 words`
    * Hallucinated response should be received
* Terminal 3
    * `cd client`
    * `./mvnw clean install`
    * Use `java -jar target/quarkus-app/quarkus-run.jar` as `<cli>` command
    * `<cli> init` - creates Milvus collection
    * `<cli> list-milvus` - one record (dummy document) returned
    * `<cli> query-chat Describe IterQR framework in 20 words`- Hallucinated response returned
    * `<cli> insert-document 2504.05309v1`
    * `<cli> list-milvus` - two records returned, one with the truncated paper
    * `<cli> query-chat Describe IterQR framework in 20 words`- Meaningful response returned
    * `<cli> delete-document 2504.05309v1`
    * `<cli> list-milvus` - one record (dummy document) returned
    * `<cli> query-chat Describe IterQR framework in 20 words`- Hallucinated response returned


https://www.youtube.com/watch?v=uxE8FFiu_UQ

https://hub.docker.com/r/ollama/ollama

I'd like to propose this scenario

There is a PostgreSQL table with TEXT field containing some text documents
** Here we can mention that Postgres also supports vector datatype for which Debezium has support but it might not be available

Changes from table are captured and embedding calculated out of the TEXT field

Embeddings are stored in Milvus database
There is an ollama LLM instance running
A Quarkus LangChain4j application will communicate with ollama LLM and use Milvus to provide RAG
As a part of demo it is demonstrated that updates and deletes have impact on LLM query results
As a strech goal or maube follow-up blogpost
The quarkus app will not use Milvus directly but act as MCP client for Milvus MCP server


