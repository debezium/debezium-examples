package io.debezium.examples.apache.beam;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import io.debezium.connector.mysql.MySqlConnector;
import org.apache.beam.io.debezium.DebeziumIO;
import org.apache.beam.io.debezium.DebeziumIO.ConnectorConfiguration;
import org.apache.beam.io.debezium.SourceRecordJson;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.TableDestination;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.ValueInSingleWindow;
import org.joda.time.DateTimeUtils;

import java.io.IOException;
import java.util.Objects;

public class Main {

  public static void main(String[] args) {
    Pipeline pipeline = Pipeline.create(PipelineOptionsFactory.fromArgs(args).withValidation().create());

    pipeline
        // Step 1. Read change events with Debezium
        .apply("ReadDatabaseEvents", DebeziumIO.readAsJson()
            .withConnectorConfiguration(ConnectorConfiguration.create()
                .withConnectorClass(MySqlConnector.class)
                .withHostName("localhost")
                .withPort("3306")
                .withUsername("debezium")
                .withPassword("dbz")
                .withConnectionProperty("database.server.id", "184054")
                .withConnectionProperty("database.include.list", "inventory")
                .withConnectionProperty("table.include.list", "inventory.customers")
                .withConnectionProperty("include.schema.changes", "false")
                .withConnectionProperty("snapshot.mode", "initial")
                .withConnectionProperty("snapshot.locking.mode", "none")
                .withConnectionProperty("tombstones.on.delete", "false")
            )
            .withFormatFunction(new SourceRecordJson.SourceRecordJsonMapper())
            .withCoder(StringUtf8Coder.of())
        )

        // Step 2. Transform the records into TableRow
        .apply("TransformToTableRow", ParDo.of(new DoFn<String, TableRow>() {
          @ProcessElement
          public void processElement(ProcessContext c) throws IOException {
            JsonNode payload = new ObjectMapper().readTree(c.element());
            TableRow record = new TableRow()
                .set("metadata", payload.get("metadata").toString())
                .set("before", payload.get("before").toString())
                .set("after", payload.get("after").toString())
                .set("table_id", payload.get("metadata").get("table").asText())
                .set("write_time", DateTimeUtils.currentTimeMillis());
            c.output(record);
          }
        }))

        // Step 3. Sink TableRow to BigQuery
        .apply("WriteToBigQuery", BigQueryIO.writeTableRows()
            .to((SerializableFunction<ValueInSingleWindow<TableRow>, TableDestination>) input -> {
              TableRow row = Objects.requireNonNull(Objects.requireNonNull(input).getValue());
              TableReference tableReference = new TableReference()
                  .setProjectId("my-project")
                  .setDatasetId("my-dataset")
                  .setTableId(row.get("table_id").toString());
              return new TableDestination(tableReference, "Customer names and emails");
            })
            .withoutValidation()
            .ignoreUnknownValues()
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_NEVER)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
            .withMethod(BigQueryIO.Write.Method.STREAMING_INSERTS));

    pipeline.run();
  }

}
