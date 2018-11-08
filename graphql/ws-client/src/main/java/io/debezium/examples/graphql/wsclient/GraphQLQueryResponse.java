/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphql.wsclient;

import java.util.HashMap;
import java.util.List;

/**
 * Represents a response coming from a GraphQL API
 *
 * @see https://graphql.org/learn/serving-over-http/#response
 *
 */
public class GraphQLQueryResponse {

	private HashMap<String, Object> data;
	private List<Object> errors;

	public Object getData() {
		return data;
	}

	public void setData(HashMap<String, Object> data) {
		this.data = data;
	}

	public List<Object> getErrors() {
		return errors;
	}

	public void setErrors(List<Object> errors) {
		this.errors = errors;
	}

	public boolean hasErrors() {
		return errors != null && !errors.isEmpty();
	}

	public String getErrorsAsFormattedJSON() {
		return JSONConverter.instance().toJSON(errors);
	}

	public String getDataAsFormattedJSON() {
		return JSONConverter.instance().toJSON(data);
	}

	@Override
	public String toString() {
		return "GraphQLQueryResponse [data=" + data + ", errors=" + errors + "]";
	}

}
