package io.debezium.examples.graphql;

import java.util.Collections;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;

import org.aerogear.kafka.cdi.annotation.Consumer;
import org.aerogear.kafka.cdi.annotation.KafkaConfig;

import io.debezium.examples.graphql.model.Order;
import io.debezium.examples.graphql.serdes.ChangeEventAwareJsonSerde;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.functions.Predicate;
import io.reactivex.observables.ConnectableObservable;

@ApplicationScoped
@KafkaConfig(bootstrapServers = "#{KAFKA_SERVICE_HOST}:#{KAFKA_SERVICE_PORT}")
public class OrderPublisher {

    private static final Logger LOG = Logger.getLogger(OrderPublisher.class.getName());

    private final ChangeEventAwareJsonSerde<Long> longKeySerde;
    private final ChangeEventAwareJsonSerde<Order> orderSerde;

    private final Flowable<Order> publisher;
    private ObservableEmitter<Order> emitter;

    public OrderPublisher() {
        longKeySerde = new ChangeEventAwareJsonSerde<>(Long.class);
        longKeySerde.configure(Collections.emptyMap(), true);

        orderSerde = new ChangeEventAwareJsonSerde<>(Order.class);
        orderSerde.configure(Collections.emptyMap(), false);

        Observable<Order> ratingObservable = Observable.create(emitter -> {
            this.emitter = emitter;
        });
        ConnectableObservable<Order> connectableObservable = ratingObservable.share().publish();
        connectableObservable.connect();

        this.publisher = connectableObservable.toFlowable(BackpressureStrategy.BUFFER);

    }

    @Consumer(topics = "dbserver1.inventory.orders", groupId = "myGroup")
    public void receive(byte[] key, byte[] data) {
        try {
            Order order = orderSerde.deserializer().deserialize("dbserver1.inventory.orders", data);
            
            if (emitter != null) {
                emitter.onNext(order);
            }

        } catch (Exception ex) {
            LOG.info("ERROR: " + ex);
            throw new RuntimeException(ex);
        }
    }

    public Flowable<Order> getPublisher() {
        return this.publisher;
    }
}
