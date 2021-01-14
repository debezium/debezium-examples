package io.debezium.examples.caching.commons;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(schemaPackageName = "caching",
      includeClasses = { ProtoOrderLine.class, ProtoOrderLineStatus.class, ProtoPurchaseOrder.class, LocalDateTimeAdapter.class})
interface PurchaseOrdersContextInitializer extends SerializationContextInitializer {
}
