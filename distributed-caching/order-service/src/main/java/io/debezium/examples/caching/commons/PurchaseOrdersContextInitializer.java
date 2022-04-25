/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.commons;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

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
interface PurchaseOrdersContextInitializer extends GeneratedSchema {
}
