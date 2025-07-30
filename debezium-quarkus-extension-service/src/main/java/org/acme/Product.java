package org.acme;

import java.math.BigDecimal;

public record Product(Integer id, String name, String  description, BigDecimal weight) { }
