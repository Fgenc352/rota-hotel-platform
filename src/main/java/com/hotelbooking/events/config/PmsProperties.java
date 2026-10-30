package com.hotelbooking.events.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.pms")
public record PmsProperties(
    @NotNull URI url,
    @NotNull Duration connectTimeout,
    @NotNull Duration requestTimeout
    ) {
    @AssertTrue(message = "PMS URL must be an absolute HTTP or HTTPS URL")
    public boolean isHttpUrl() {
        return url == null || (url.getHost() != null
            && ("http".equalsIgnoreCase(url.getScheme()) || "https".equalsIgnoreCase(url.getScheme())));
    }

    @AssertTrue(message = "PMS timeouts must be positive")
    public boolean isTimeoutsPositive() {
        return (connectTimeout == null || connectTimeout.isPositive())
        && (requestTimeout == null || requestTimeout.isPositive());
    }
}
