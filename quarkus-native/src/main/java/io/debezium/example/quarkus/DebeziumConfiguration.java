package io.debezium.example.quarkus;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;

import java.util.Map;

@StaticInitSafe
@ConfigMapping(prefix = "debezium")
public interface DebeziumConfiguration {
    Map<String, String> configuration();
}
