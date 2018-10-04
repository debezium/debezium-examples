package io.debezium.examples.graphql;

import java.util.logging.Logger;

import org.reactivestreams.Publisher;

import com.coxautodev.graphql.tools.GraphQLSubscriptionResolver;

import io.debezium.examples.graphql.model.Order;

public class SubscriptionResolver implements GraphQLSubscriptionResolver {

    private static final Logger LOG = Logger.getLogger(SubscriptionResolver.class.getName());

    private OrderPublisher publisher;

    public SubscriptionResolver(OrderPublisher publisher) {
        this.publisher = publisher;
    }

    public Publisher<Order> onNewOrder(final Integer withMinQuantity) {
        LOG.info("New Subscription for orders " + (withMinQuantity != null ? "with at least " + withMinQuantity + " quantity" : ""));
        if (withMinQuantity == null) {
            return this.publisher.getPublisher();
        }

        return this.publisher.getPublisher().filter(o -> o.quantity >= withMinQuantity);
    }
}
