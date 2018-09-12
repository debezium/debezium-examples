/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.aggregation.connect;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.JsonSchemaFactory;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.fasterxml.jackson.module.jsonSchema.factories.VisitorContext;
import com.fasterxml.jackson.module.jsonSchema.factories.WrapperFactory;
import com.fasterxml.jackson.module.jsonSchema.types.AnySchema;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import com.fasterxml.jackson.module.jsonSchema.types.BooleanSchema;
import com.fasterxml.jackson.module.jsonSchema.types.IntegerSchema;
import com.fasterxml.jackson.module.jsonSchema.types.NumberSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;

/**
 * Emits Kafka Connect's JSON schema format instead of regular JSON Schema.
 *
 * @author Gunnar Morling
 */
public class KafkaConnectSchemaFactoryWrapper extends SchemaFactoryWrapper {

    private static class KafkaConnectSchemaFactoryWrapperFactory extends WrapperFactory {
        @Override
        public SchemaFactoryWrapper getWrapper(SerializerProvider p) {
            SchemaFactoryWrapper wrapper = new KafkaConnectSchemaFactoryWrapper();
            if (p != null) {
                wrapper.setProvider(p);
            }
            return wrapper;
        };

        @Override
        public SchemaFactoryWrapper getWrapper(SerializerProvider p, VisitorContext rvc) {
            SchemaFactoryWrapper wrapper = new KafkaConnectSchemaFactoryWrapper();
            if (p != null) {
                wrapper.setProvider(p);
            }
            wrapper.setVisitorContext(rvc);
            return wrapper;
        }
    };

    public KafkaConnectSchemaFactoryWrapper() {
        super(new KafkaConnectSchemaFactoryWrapperFactory());
        schemaProvider = new KafkaConnectSchemaAdapterFactory();
    }

    public interface KafkaConnectSchemaAdapter {
        String getConnectType();

        boolean isOptional();

        String getField();

        String getName();
    }

    public static class KafkaConnectObjectSchemaAdapter extends ObjectSchema implements KafkaConnectSchemaAdapter {

        private String field;

        @Override
        public String getConnectType() {
            return "struct";
        }

        @Override
        public boolean isOptional() {
            return !Boolean.TRUE.equals(getRequired());
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public void enrichWithBeanProperty(BeanProperty beanProperty) {
            field = beanProperty.getName();
        }

        public KafkaConnectSchemaAdapter[] getFields() {
            return getProperties().values().toArray(new KafkaConnectSchemaAdapter[0]);
        }

        @Override
        public String getName() {
            return getId();
        }
    }

    public static class KafkaConnectArraySchemaAdapter extends ArraySchema implements KafkaConnectSchemaAdapter {

        private String field;
        private boolean isByteArray;

        @Override
        public String getConnectType() {
            return isByteArray ? "bytes" : "array";
        }

        @Override
        public boolean isOptional() {
            return !Boolean.TRUE.equals(getRequired());
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public void enrichWithBeanProperty(BeanProperty beanProperty) {
            field = beanProperty.getName();

            if (beanProperty.getType().isArrayType()) {
                if (beanProperty.getType().getContentType().getRawClass() == byte.class) {
                    isByteArray = true;
                } else {
                    getItems().asSingleItems().getSchema().enrichWithBeanProperty(beanProperty);
                }
            }
        }

        public KafkaConnectSchemaAdapter getConnectItems() {
            if (getItems().isSingleItems()) {
                return (KafkaConnectSchemaAdapter) getItems().asSingleItems().getSchema();
            } else {
                throw new UnsupportedOperationException("Mixed array item types are disallowed");
            }
        }

        @Override
        public String getName() {
            return getId();
        }

        public boolean isByteArray() {
            return isByteArray;
        }

        @Override
        public void setItemsSchema(JsonSchema jsonSchema) {
            if (jsonSchema instanceof NumberSchema) {
                super.setItemsSchema(new KafkaConnectNumberSchemaAdapter());
            } else if (jsonSchema instanceof IntegerSchema) {
                super.setItemsSchema(new KafkaConnectIntegerSchemaAdapter());
            } else if (jsonSchema instanceof StringSchema) {
                super.setItemsSchema(new KafkaConnectStringSchemaAdapter());
            } else if (jsonSchema instanceof BooleanSchema) {
                super.setItemsSchema(new KafkaConnectBooleanSchemaAdapter());
            } else {
                super.setItemsSchema(jsonSchema);
            }
        }
    }

    public static class KafkaConnectIntegerSchemaAdapter extends IntegerSchema implements KafkaConnectSchemaAdapter {

        private String connectType;
        private String field;

        @Override
        public void enrichWithBeanProperty(BeanProperty beanProperty) {
            field = beanProperty.getName();

            if (beanProperty.getType().getRawClass() == byte.class
                    || beanProperty.getType().getRawClass() == Byte.class) {
                connectType = "int8";
            } else if (beanProperty.getType().getRawClass() == short.class
                    || beanProperty.getType().getRawClass() == Short.class) {
                connectType = "int16";
            } else if (beanProperty.getType().getRawClass() == int.class
                    || beanProperty.getType().getRawClass() == Integer.class) {
                connectType = "int32";
            } else if (beanProperty.getType().getRawClass() == long.class
                    || beanProperty.getType().getRawClass() == Long.class) {
                connectType = "int64";
            } else {
                throw new UnsupportedOperationException("Property of unsupported integer type: " + beanProperty);
            }
        }

        @Override
        public String getConnectType() {
            return connectType;
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public boolean isOptional() {
            return !Boolean.TRUE.equals(getRequired());
        }

        @Override
        public String getName() {
            return null;
        }
    }

    public static class KafkaConnectNumberSchemaAdapter extends NumberSchema implements KafkaConnectSchemaAdapter {

        private String connectType;
        private String field;

        @Override
        public void enrichWithBeanProperty(BeanProperty beanProperty) {
            field = beanProperty.getName();

            if (beanProperty.getType().getRawClass() == float.class
                    || beanProperty.getType().getRawClass() == Float.class
                    || beanProperty.getType().getRawClass() == float[].class
                    || beanProperty.getType().getRawClass() == Float[].class) {
                connectType = "float";
            } else if (beanProperty.getType().getRawClass() == double.class
                    || beanProperty.getType().getRawClass() == Double.class
                    || beanProperty.getType().getRawClass() == double[].class
                    || beanProperty.getType().getRawClass() == Double[].class) {
                connectType = "double";
            }
            // TODO BigDecimal -> Decimal via custom annotation for specifying scale
            else {
                throw new UnsupportedOperationException("Property of unsupported numeric type: " + beanProperty);
            }
        }

        @Override
        public String getConnectType() {
            return connectType;
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public boolean isOptional() {
            return !Boolean.TRUE.equals(getRequired());
        }

        @Override
        public String getName() {
            return null;
        }
    }

    public static class KafkaConnectStringSchemaAdapter extends StringSchema implements KafkaConnectSchemaAdapter {

        private String field;

        @Override
        public void enrichWithBeanProperty(BeanProperty beanProperty) {
            field = beanProperty.getName();
        }

        @Override
        public String getConnectType() {
            return "string";
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public boolean isOptional() {
            return !Boolean.TRUE.equals(getRequired());
        }

        @Override
        public String getName() {
            return null;
        }
    }

    public static class KafkaConnectBooleanSchemaAdapter extends BooleanSchema implements KafkaConnectSchemaAdapter {

        private String field;

        @Override
        public void enrichWithBeanProperty(BeanProperty beanProperty) {
            field = beanProperty.getName();
        }

        @Override
        public String getConnectType() {
            return "boolean";
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public boolean isOptional() {
            return !Boolean.TRUE.equals(getRequired());
        }

        @Override
        public String getName() {
            return null;
        }
    }

    public static class KafkaConnectAnySchemaAdapter extends AnySchema implements KafkaConnectSchemaAdapter {

        private String connectType;
        private String field;
        private String name;

        @Override
        public void enrichWithBeanProperty(BeanProperty beanProperty) {
            field = beanProperty.getName();

            if (beanProperty.getType().getRawClass() == LocalDate.class) {
                connectType = "int32";
                name = "io.debezium.time.Timestamp";
            }
            else {
                throw new UnsupportedOperationException("Property of unsupported type: " + beanProperty);
            }
        }

        @Override
        public String getConnectType() {
            return connectType;
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public boolean isOptional() {
            return !Boolean.TRUE.equals(getRequired());
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public static class KafkaConnectSchemaAdapterFactory extends JsonSchemaFactory {

        @Override
        public BooleanSchema booleanSchema() {
            return new KafkaConnectBooleanSchemaAdapter();
        }

        @Override
        public StringSchema stringSchema() {
            return new KafkaConnectStringSchemaAdapter();
        }

        @Override
        public IntegerSchema integerSchema() {
            return new KafkaConnectIntegerSchemaAdapter();
        }

        @Override
        public NumberSchema numberSchema() {
            return new KafkaConnectNumberSchemaAdapter();
        }

        @Override
        public ObjectSchema objectSchema() {
            return new KafkaConnectObjectSchemaAdapter();
        }

        @Override
        public ArraySchema arraySchema() {
            return new KafkaConnectArraySchemaAdapter();
        }

        @Override
        public AnySchema anySchema() {
            return new KafkaConnectAnySchemaAdapter();
        }
    }
}
