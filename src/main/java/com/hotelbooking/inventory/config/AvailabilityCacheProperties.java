package com.hotelbooking.inventory.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.availability-cache")
public record AvailabilityCacheProperties(
    @DefaultValue("memory") @NotNull Provider provider,
    @DefaultValue("5s") @NotNull Duration ttl,
    @DefaultValue("2000") @Min(1) int maxEntries,
    @DefaultValue("hotel:availability:v1") @NotBlank String namespace
    ) {
    public enum Provider {
        MEMORY, REDIS
    }

    @AssertTrue(message = "Availability cache TTL must be positive")
    public boolean isTtlPositive() {
        return ttl == null || ttl.isPositive();
    }
}
