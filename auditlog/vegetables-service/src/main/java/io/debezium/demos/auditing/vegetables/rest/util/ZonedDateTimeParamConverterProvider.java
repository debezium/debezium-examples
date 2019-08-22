package io.debezium.demos.auditing.vegetables.rest.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class ZonedDateTimeParamConverterProvider implements ParamConverterProvider {

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType == ZonedDateTime.class) {
            return (ParamConverter<T>) new ZonedDateTimeParamConverter();
        }

        return null;
    }
}
