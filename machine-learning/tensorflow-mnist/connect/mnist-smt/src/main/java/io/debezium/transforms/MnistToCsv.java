package io.debezium.transforms;

import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;

public class MnistToCsv<R extends ConnectRecord<R>> implements Transformation<R> {

    private static final String LABEL_CONFIG = "label_column";
    private static final String PIXEL_CONFIG = "pixels_column";
    private static final String LABEL_COLUMN_DEFAULT = "label";
    private static final String PIXELS_COLUMN_DEFAULT = "pixels";

    private String labelFieldName;
    private String pixlesFieldName;

    @Override
    public R apply(R r) {
        final Struct value = (Struct) r.value();
        String key = value.getInt16(labelFieldName).toString();

        StringBuilder builder = new StringBuilder();
        for (byte pixel : value.getBytes(pixlesFieldName)) {
            builder.append(pixel & 0xFF).append(",");
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        String newValue = builder.toString();

        return r.newRecord(r.topic(), r.kafkaPartition(), Schema.STRING_SCHEMA, key, Schema.STRING_SCHEMA, newValue, r.timestamp());
    }

    @Override
    public void configure(Map<String, ?> map) {
        labelFieldName = map.containsKey(LABEL_CONFIG) ? (String) map.get(LABEL_CONFIG) : LABEL_COLUMN_DEFAULT;
        pixlesFieldName = map.containsKey(PIXEL_CONFIG) ? (String) map.get(PIXEL_CONFIG) : PIXELS_COLUMN_DEFAULT;
    }

    @Override
    public ConfigDef config() {
        return new ConfigDef();
    }

    @Override
    public void close() {
    }
}

