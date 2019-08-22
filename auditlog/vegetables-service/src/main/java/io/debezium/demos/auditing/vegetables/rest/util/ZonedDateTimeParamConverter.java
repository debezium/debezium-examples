package io.debezium.demos.auditing.vegetables.rest.util;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.ws.rs.ext.ParamConverter;

public class ZonedDateTimeParamConverter implements ParamConverter<ZonedDateTime> {

    @Override
    public ZonedDateTime fromString(String value) {
        return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    @Override
    public String toString(ZonedDateTime value) {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(value);
    }
}
