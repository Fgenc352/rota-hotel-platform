package com.hotelbooking.inventory.application.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Night( LocalDate date, BigDecimal price) {
}
