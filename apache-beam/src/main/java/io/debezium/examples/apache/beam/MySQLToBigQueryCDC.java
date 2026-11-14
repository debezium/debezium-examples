package io.debezium.examples.apache.beam;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import io.debezium.connector.mysql.MySqlConnector;
import org.apache.beam.io.debezium.DebeziumIO;
import org.apache.beam.io.debezium.DebeziumIO.ConnectorConfiguration;
import org.apache.beam.io.debezium.SourceRecordJson;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.RowMutationInformation;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.ValueKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MySQLToBigQueryCDC {

  private static final Logger log = LoggerFactory.getLogger(MySQLToBigQueryCDC.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String OP_TYPE = "op_type";
  private static final String SEQ_NUM = "seq_num";

  private static final TableSchema tableSchema = new TableSchema().setFields(List.of(
      new TableFieldSchema().setName("id").setType("INTEGER").setMode("REQUIRED"),
      new TableFieldSchema().setName("first_name").setType("STRING"),
      new TableFieldSchema().setName("last_name").setType("STRING"),
      new TableFieldSchema().setName("email").setType("STRING")));

  public interface Options extends GcpOptions {
    @Description("BigQuery dataset of the customers table")
    @Validation.Required
    String getDataset();

    void setDataset(String dataset);
  }

  public static void main(String[] args) {
    Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);
    Pipeline pipeline = Pipeline.create(options);

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
                .withConnectionProperty("snapshot.mode", "always")
                .withConnectionProperty("snapshot.locking.mode", "none")
                .withConnectionProperty("tombstones.on.delete", "false")
            )
            .withFormatFunction(new SourceRecordJson.SourceRecordJsonMapper())
            .withFullMetadata(true)
            .withCoder(StringUtf8Coder.of()))

        // Step 2. Transform the records into TableRow
        .apply("TransformToTableRow", ParDo.of(new DoFn<String, TableRow>() {
          @ProcessElement
          public void processElement(ProcessContext processContext, OutputReceiver<TableRow> outputReceiver) throws IOException {
            JsonNode payload = MAPPER.readTree(processContext.element());
            JsonNode metadata = payload.path("metadata");
            String op = metadata.path("op").asText();

            JsonNode fields;
            ValueKind valueKind;
            switch (op) {
              case "r":
              case "c":
                fields = payload.path("after").path("fields");
                valueKind = ValueKind.INSERT;
                break;
              case "u":
                fields = payload.path("after").path("fields");
                valueKind = ValueKind.UPDATE_AFTER;
                break;
              case "d":
                fields = payload.path("before").path("fields");
                valueKind = ValueKind.DELETE;
                break;
              default:
                log.warn("Skipping unknown operation '{}': {}", op, payload);
                return;
            }
            TableRow tableRow = new TableRow()
                .set("id", fields.path("id").asLong())
                .set("first_name", fields.path("first_name").asText(null))
                .set("last_name", fields.path("last_name").asText(null))
                .set("email", fields.path("email").asText(null))
                .set(OP_TYPE, valueKind.name())
                .set(SEQ_NUM, changeSequenceNumber(metadata));

            outputReceiver.builder(tableRow).setValueKind(valueKind).output();
          }
        }))

        // Step 3. Sink TableRow to BigQuery
        .apply("WriteToBigQuery", BigQueryIO.writeTableRows()
            .to(new TableReference()
                .setProjectId(options.getProject())
                .setDatasetId(options.getDataset())
                .setTableId("customers"))
            .withTableDescription("Customer names and emails")
            .withFormatFunction(row -> {
              TableRow columns = row.clone();
              columns.remove(OP_TYPE);
              columns.remove(SEQ_NUM);
              return columns;
            })
            .withoutValidation()
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withSchema(tableSchema)
            .withPrimaryKey(Collections.singletonList("id"))
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
            .withRowMutationInformationFn(row -> RowMutationInformation.of(
                ValueKind.DELETE.name().equals(row.get(OP_TYPE))
                    ? RowMutationInformation.MutationType.DELETE
                    : RowMutationInformation.MutationType.UPSERT,
                row.get(SEQ_NUM).toString()))
            .withMethod(BigQueryIO.Write.Method.STORAGE_API_AT_LEAST_ONCE));

    pipeline.run();
  }

  /**
   * BigQuery {@code _CHANGE_SEQUENCE_NUMBER} built from the MySQL binlog coordinates, which totally order
   * the changes: {@code <file index>/<position>/<row in event>}, each section in hex.
   *
   * @see RowMutationInformation#of(RowMutationInformation.MutationType, String)
   */
  private static String changeSequenceNumber(JsonNode metadata) {
    String file = metadata.path("file").asText(); // e.g. mysql-bin.000003
    long fileIndex = Long.parseLong(file.substring(file.lastIndexOf('.') + 1));
    return Long.toHexString(fileIndex)
        + "/" + Long.toHexString(metadata.path("pos").asLong())
        + "/" + Long.toHexString(metadata.path("row").asLong());
  }
}
