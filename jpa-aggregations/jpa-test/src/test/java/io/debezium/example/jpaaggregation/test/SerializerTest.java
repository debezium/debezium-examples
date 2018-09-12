/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.example.jpaaggregation.test;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.json.JsonConverter;
import org.junit.Test;

import com.example.domain.Customer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import io.debezium.aggregation.connect.KafkaConnectSchemaFactoryWrapper;
import io.debezium.aggregation.hibernate.ObjectMapperFactory;

public class SerializerTest {

    @Test
    public void simpleJsonSerialization() throws Exception {
        Customer c = new Customer();
        c.someBlob = new byte[] {1,2,3};
        c.scores = new double[] {8.9, 42.0};
        c.tags = new String[] { "foo", "bar" };
        c.birthday = LocalDate.of(1978, Month.APRIL, 12);

        ObjectMapper mapper = new ObjectMapperFactory().buildObjectMapper();

        System.out.println(mapper.writeValueAsString(c));
    }

	@Test
	public void connectSchemaExample() throws Exception {
	    Schema schema = SchemaBuilder.struct()
	        .field("mystring", SchemaBuilder.STRING_SCHEMA)
	        .field("myarr", SchemaBuilder.array(SchemaBuilder.INT64_SCHEMA))
	        .field("mybytearr", SchemaBuilder.bytes())
	        .field("category", SchemaBuilder.struct().name("Category").field("id", SchemaBuilder.INT64_SCHEMA))
	        .build();

	    JsonConverter jsonConverter = new JsonConverter();
	    HashMap<String, Object> configs = new HashMap<>();
	    configs.put("converter.type", "value");
        jsonConverter.configure(configs);
        ObjectNode node = jsonConverter.asJsonSchema(schema);
	    System.out.println(node);
	}

	@Test
	public void simpleSchemaSerialization() throws Exception {
	    ObjectMapper mapper = new ObjectMapperFactory().buildObjectMapper();

	    KafkaConnectSchemaFactoryWrapper visitor = new KafkaConnectSchemaFactoryWrapper();
        mapper.acceptJsonFormatVisitor(Customer.class, visitor);
        JsonSchema schema1 = visitor.finalSchema();
        String schema = mapper.writeValueAsString(schema1);

        JsonConverter jsonConverter = new JsonConverter();
        HashMap<String, Object> configs = new HashMap<>();
        configs.put("converter.type", "value");
        jsonConverter.configure(configs);
        String envelope = "{ "
                    + schema.substring(2, schema.length() -2) + ","
                    + " \"payload\" : { "
                        + "\"id\" : 1001, "
                        + "\"firstName\" : \"Bob\", "
                        + "\"lastName\" : \"Smith\", "
                        + "\"email\" : \"mail@example.com\", "
                        + "\"active\" : true, "
                        + "\"someBlob\" : \"U29tZSBiaW5hcnkgYmxvYg==\", "
                        + "\"addresses\" : {}, "
                        + "\"scores\" : [ 8.9, 42.0 ], "
                        + "\"tags\" : [ \"foo\", \"bar\" ], "
                        + "\"category\" : {"
                            + "\"id\" : 123,"
                            + "\"name\" : \"B2B\""
                        + "},"
                        + "\"birthday\" : 3023"
                    + "} "
                + "}";

        System.out.println(envelope);

        SchemaAndValue sav = jsonConverter.toConnectData("dummy", envelope.getBytes());

        System.out.println(sav);
	}
}
