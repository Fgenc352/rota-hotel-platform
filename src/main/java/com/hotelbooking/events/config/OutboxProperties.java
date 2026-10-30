package com.hotelbooking.events.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.outbox")
public record OutboxProperties(@Min(1) int batchSize, @NotNull Duration pollInterval) {
    @AssertTrue(message = "Outbox poll interval must be positive")
    public boolean isPollIntervalPositive() {
        return pollInterval == null || pollInterval.isPositive();
    }
}
