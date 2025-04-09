package io.debezium.examples.airag;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq.CollectionSchema;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MilvusStore {
    @ConfigProperty(name = "milvus.uri")
    String milvusUri;

    @ConfigProperty(name = "milvus.collection.name")
    String milvusCollectionName;

    private MilvusClientV2 client;

    @PostConstruct
    void connect() {
        Log.infof("Connecting to Milvus at %s", milvusUri);

        final var config = ConnectConfig.builder()
                .uri(milvusUri)
                .build();
        client = new MilvusClientV2(config);
    }

    public void init() {
        try {
            client.dropCollection(DropCollectionReq.builder().collectionName(milvusCollectionName).build());
        }
        catch (Exception e) {
            // Ignore drop errors for non-existing collection
        }

        final var pkField = CreateCollectionReq.FieldSchema.builder()
                .name("id")
                .isPrimaryKey(true)
                .dataType(DataType.VarChar)
                .maxLength(64)
                .build();
        final var titleField = CreateCollectionReq.FieldSchema.builder()
                .name("metadata")
                .dataType(DataType.JSON)
                .isNullable(true)
                .build();
        final var contentsField = CreateCollectionReq.FieldSchema.builder()
                .name("text")
                .dataType(DataType.VarChar)
                .build();
        final var vectorField = CreateCollectionReq.FieldSchema.builder()
                .name("vector")
                .dataType(DataType.FloatVector)
                .dimension(2048)
                .build();
        final var collectionSchema = CollectionSchema.builder()
                .fieldSchemaList(List.of(pkField, titleField, contentsField, vectorField))
                .build();
        final var index = IndexParam.builder()
                .fieldName("vector")
                .indexType(IndexParam.IndexType.AUTOINDEX)
                .build();
        final var request = CreateCollectionReq.builder()
                .collectionName(milvusCollectionName)
                .collectionSchema(collectionSchema)
                .indexParams(List.of(index))
                .build();
        client.createCollection(request);

        Log.infof("Created collection '%s' with schema: %s", milvusCollectionName, collectionSchema);
        client.close();
    }

    public void list() {
        final var request = QueryReq.builder()
                .collectionName(milvusCollectionName)
                .filter("id like \"%\"")
                .build();

        final var response = client.query(request);
        Log.infof("Milvus list results size: %d", response.getQueryResults().size());
        response.getQueryResults().forEach(result -> {
            Log.infof("Document: %s", result.getEntity());
        });

        client.close();
    }
}
