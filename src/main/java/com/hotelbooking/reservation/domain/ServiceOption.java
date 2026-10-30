package com.hotelbooking.reservation.domain;

import java.math.BigDecimal;

public record ServiceOption(
    String code,
    String name,
    String description,
    BigDecimal price,
    ServiceUnit unit,
    boolean included
    ) {
}
