package com.hotelbooking.reservation.domain;

import java.math.BigDecimal;

/** Immutable price/name snapshot retained when the service catalog changes. */ public record ServiceLine(
    String code,
    String name,
    ServiceUnit unit,
    BigDecimal unitPrice,
    long quantity,
    BigDecimal total,
    boolean included
    ) {
}
