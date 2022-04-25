/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.commons;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

@ProtoAdapter(LocalDateTime.class)
public class LocalDateTimeAdapter {

   @ProtoFactory
   LocalDateTime create(Long millis) {
      return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime();
   }

   @ProtoField(number = 1, type = Type.FIXED64)
   Long millis(LocalDateTime localDateTime) {
      return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
   }
}
