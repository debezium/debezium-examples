/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.commons;

import java.math.BigDecimal;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

@ProtoAdapter(BigDecimal.class)
public class BigDecimalAdapter {

   @ProtoFactory
   BigDecimal create(Double value) {
      return BigDecimal.valueOf(value);
   }

   @ProtoField(number = 1, type = Type.DOUBLE, defaultValue = "0")
   Double value(BigDecimal value) {
      return value.doubleValue();
   }
}
