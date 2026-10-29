///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.quarkus.platform:quarkus-bom:3.28.4@pom
//DEPS io.quarkus:quarkus-arc
//DEPS io.debezium.quarkus:debezium-quarkus-postgres:3.7.0.Final
//DEPS org.apache.kafka:kafka-clients:4.3.1
//DEPS dev.hardwood:hardwood-core:1.1.0.Beta1
//FILES application.properties
//JAVAC_OPTIONS -parameters
//JAVA_OPTIONS -Djava.util.logging.manager=org.jboss.logmanager.LogManager
//JAVA 21+

import dev.hardwood.OutputFile;
import dev.hardwood.metadata.LogicalType;
import dev.hardwood.metadata.PhysicalType;
import dev.hardwood.metadata.RepetitionType;
import dev.hardwood.schema.FileSchema;
import dev.hardwood.writer.ParquetFileWriter;
import dev.hardwood.writer.RowWriter;

import io.debezium.runtime.Capturing;
import io.debezium.runtime.CapturingEvent;
import io.debezium.runtime.CapturingEvent.Create;
import io.debezium.runtime.CapturingEvent.Read;
import io.quarkus.runtime.StartupEvent;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class DebeziumParquet {

  private static final Path OUTPUT_DIR = Path.of("parquet-output");

  private static final FileSchema SCHEMA = FileSchema.builder("customers")
      .addColumn("id", PhysicalType.INT32, RepetitionType.REQUIRED)
      .addColumn("first_name", PhysicalType.BYTE_ARRAY, RepetitionType.REQUIRED, new LogicalType.StringType())
      .addColumn("last_name", PhysicalType.BYTE_ARRAY, RepetitionType.REQUIRED, new LogicalType.StringType())
      .addColumn("email", PhysicalType.BYTE_ARRAY, RepetitionType.REQUIRED, new LogicalType.StringType())
      .build();

  private ParquetFileWriter parquetFile;
  private RowWriter rowWriter;

  void onStart(@Observes StartupEvent event) throws IOException {
    Files.createDirectories(OUTPUT_DIR);
    parquetFile = ParquetFileWriter.create(OutputFile.of(OUTPUT_DIR.resolve("customers.parquet")), SCHEMA);
    rowWriter = parquetFile.rowWriter();
  }

  @PreDestroy
  void cleanup() throws Exception {
    parquetFile.close();
  }

  @Capturing
  public void capture(CapturingEvent<SourceRecord, SourceRecord> event) throws IOException {
    if (event instanceof Read<SourceRecord, SourceRecord> e) {
      Struct after = ((Struct) e.record().value()).getStruct("after");

      rowWriter.writeRow(row -> row
          .setInt("id", after.getInt32("id"))
          .setString("first_name", after.getString("first_name"))
          .setString("last_name", after.getString("last_name"))
          .setString("email", after.getString("email")));
    }
  }
}
