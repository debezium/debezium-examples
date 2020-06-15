/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.example.server.mapper;

import javax.enterprise.context.Dependent;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.debezium.server.StreamNameMapper;

@Dependent
public class PrefixingNameMapper implements StreamNameMapper {

    @ConfigProperty(name = "mapper.prefix")
    String prefix;

    @Override
    public String map(String topic) {
        return prefix + "." + topic;
    }

}
