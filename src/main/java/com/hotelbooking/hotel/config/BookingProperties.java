package com.hotelbooking.hotel.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.booking")
public record BookingProperties(
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal eurRate
    ) {
}
