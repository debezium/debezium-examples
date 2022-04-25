/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.commons;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@ProtoAdapter(BigDecimal.class)
public final class BigDecimalAdapter {

   @ProtoFactory
   BigDecimal create(byte[] unscaledValue, int scale) {
      return new BigDecimal(new BigInteger(unscaledValue), scale);
   }

   @ProtoField(1)
   byte[] getUnscaledValue(BigDecimal bigDecimal) {
      return bigDecimal.unscaledValue().toByteArray();
   }

   @ProtoField(value = 2, defaultValue = "0")
   int getScale(BigDecimal bigDecimal) {
      return bigDecimal.scale();
   }
}
