/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.example.quarkus;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;

import java.util.Map;

@StaticInitSafe
@ConfigMapping(prefix = "debezium")
public interface DebeziumConfiguration {
    Map<String, String> configuration();
}
