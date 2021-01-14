package io.debezium.examples.caching.commons;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@ProtoAdapter(LocalDateTime.class)
public class LocalDateTimeAdapter {

   @ProtoFactory
   LocalDateTime create(Long millis) {
      return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime();
   }

   @ProtoField(number = 1, type = Type.FIXED64, defaultValue = "0")
   Long millis(LocalDateTime localDateTime) {
      return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
   }
}