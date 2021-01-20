package io.debezium.examples.caching;

import io.debezium.examples.caching.commons.BigDecimalAdapter;
import io.debezium.examples.caching.commons.LocalDateTimeAdapter;
import io.debezium.examples.caching.commons.OrderLine;
import io.debezium.examples.caching.commons.OrderLineStatus;
import io.debezium.examples.caching.commons.PurchaseOrder;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(schemaPackageName = "caching",
      includeClasses = {
            OrderLine.class,
            OrderLineStatus.class,
            PurchaseOrder.class,
            BigDecimalAdapter.class,
            LocalDateTimeAdapter.class})
interface PurchaseOrdersContextInitializer extends SerializationContextInitializer {
}
