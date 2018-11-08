/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphql.wsclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JSONConverter {

	private static JSONConverter theConverter;

	public static synchronized JSONConverter instance() {
		if (theConverter == null) {
			theConverter = new JSONConverter();
		}

		return theConverter;
	}

	private final ObjectMapper objectMapper;

	private JSONConverter() {
		this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
	}

	public String toJSON(Object object) {
		try {
			return objectMapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Could not convert to JSON: " + e, e);
		}
	}

	public <T> T toObject(String json, Class<T> clazz) {
		try {
			return objectMapper.readValue(json, clazz);
		} catch (Exception e) {
			throw new RuntimeException("Could not convert JSON to Object: " + e, e);
		}
	}
}
