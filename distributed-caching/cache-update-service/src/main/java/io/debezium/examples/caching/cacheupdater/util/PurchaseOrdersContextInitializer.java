/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.cacheupdater.util;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

import io.debezium.examples.caching.commons.BigDecimalAdapter;
import io.debezium.examples.caching.commons.LocalDateTimeAdapter;
import io.debezium.examples.caching.model.OrderLine;
import io.debezium.examples.caching.model.OrderLineStatus;
import io.debezium.examples.caching.model.PurchaseOrder;

@AutoProtoSchemaBuilder(schemaPackageName = "caching",
      includeClasses = {
            OrderLine.class,
            OrderLineStatus.class,
            PurchaseOrder.class,
            BigDecimalAdapter.class,
            LocalDateTimeAdapter.class})
interface PurchaseOrdersContextInitializer extends SerializationContextInitializer {
}
