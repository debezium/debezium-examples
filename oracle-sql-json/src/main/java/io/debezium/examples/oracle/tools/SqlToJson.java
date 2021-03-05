package io.debezium.examples.oracle.tools;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.connector.oracle.antlr.OracleDdlParser;
import io.debezium.relational.TableId;
import io.debezium.relational.Tables;
import io.debezium.relational.history.JsonTableChangeSerializer;
import io.debezium.relational.history.TableChanges;
import io.debezium.text.MultipleParsingExceptions;
import io.debezium.text.ParsingException;

/**
 * A simple tool that converts Oracle CREATE DDL into JSON based model.
 */
public class SqlToJson {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlToJson.class);

    private static final String PROP_CATALOG = "sql.catalog";
    private static final String PROP_SCHEMA = "sql.schema";
    private static final String PROP_FILE = "sql.file";

    public static void main(String[] args) throws Exception {
        final String catalog = System.getProperty(PROP_CATALOG);
        final String schema = System.getProperty(PROP_SCHEMA);
        final String file = System.getProperty(PROP_FILE);

        if (catalog == null || catalog.isEmpty()) {
            LOGGER.error("Property '{}' must contain the name of database catalog", PROP_CATALOG);
            System.exit(1);
        }
        if (schema == null || schema.isEmpty()) {
            LOGGER.error("Property '{}' must contain the name of database schema", PROP_SCHEMA);
            System.exit(1);
        }
        if (file == null || file.isEmpty()) {
            LOGGER.error("Property '{}' must contain the name of the file with DDL statement(s)", PROP_FILE);
            System.exit(1);
        }

        final OracleDdlParser parser = new OracleDdlParser(true, catalog, schema);
        final Tables tables = new Tables();
        final TableChanges changes = new TableChanges();
        final JsonTableChangeSerializer serializer = new JsonTableChangeSerializer();

        try {
            parser.parse(new String(Files.readAllBytes(Paths.get(file))), tables);
        }
        catch (MultipleParsingExceptions e) {
            e.getErrors().forEach(x -> LOGGER.error("Error in parsing", x));
            System.exit(1);
        }
        catch (ParsingException e) {
            LOGGER.error("Error in parsing", e);
            System.exit(1);
        }
        for (TableId id: tables.tableIds()) {
            changes.alter(tables.forTable(id));
        }
        System.out.println(serializer.serialize(changes));
    }
}
