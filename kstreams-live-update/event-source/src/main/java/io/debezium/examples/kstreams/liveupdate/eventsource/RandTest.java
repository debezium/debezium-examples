package io.debezium.examples.kstreams.liveupdate.eventsource;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Random;

public class RandTest {

    public static void main(String[] args) {
        Random random = new Random();

        MathContext mc = new MathContext(2);
        for(int i = 0; i < 100; i++) {
            BigDecimal bigDecimal = BigDecimal.valueOf(40 * random.nextDouble());
            bigDecimal = bigDecimal.setScale(1, RoundingMode.HALF_UP);
            System.out.println(bigDecimal.doubleValue());
        }
    }
}
