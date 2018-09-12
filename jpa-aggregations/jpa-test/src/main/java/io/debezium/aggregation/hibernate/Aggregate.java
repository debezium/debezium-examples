/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.aggregation.hibernate;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "aggregates")
public class Aggregate {

    @EmbeddedId
    public AggregateKey pk;

    public String keySchema;

    public String valueSchema;

    public String materialization;

    @Embeddable
    public static class AggregateKey implements Serializable {

        public String rootId;

        public String rootType;

        public AggregateKey() {
        }

        public AggregateKey(String rootId, String rootType) {
            this.rootId = rootId;
            this.rootType = rootType;
        }

        @Override
        public String toString() {
            return "AggregateKey [rootId=" + rootId + ", rootType=" + rootType + "]";
        }
    }
}
