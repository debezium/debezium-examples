package io.debezium.examples.graphql;

import java.util.logging.Logger;

import org.reactivestreams.Publisher;

import com.coxautodev.graphql.tools.GraphQLSubscriptionResolver;

import io.debezium.examples.graphql.model.Order;
import io.reactivex.Flowable;

public class SubscriptionResolver implements GraphQLSubscriptionResolver {

    private static final Logger LOG = Logger.getLogger(SubscriptionResolver.class.getName());

    private OrderPublisher orderPublisher;

    public SubscriptionResolver(OrderPublisher publisher) {
        this.orderPublisher = publisher;
    }

    public Publisher<Order> onNewOrder(final Integer withMinQuantity, final String withProductId ) {
        LOG.info("New Subscription for orders " + (withMinQuantity != null ? "with at least " + withMinQuantity + " quantity" : "")
                + (withMinQuantity != null ? " product id " + withProductId  : ""));
        
        Flowable<Order> publisher = this.orderPublisher.getPublisher();
        
        if (withMinQuantity != null) {
            publisher = publisher.filter(o -> o.quantity >= withMinQuantity );
        }
        
        if (withProductId != null) {
            publisher = publisher.filter(o -> withProductId.equals(String.valueOf(o.productId)));
        }
        
        return publisher;
    }
}
