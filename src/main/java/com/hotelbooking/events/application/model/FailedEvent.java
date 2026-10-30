package com.hotelbooking.events.application.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FailedEvent(
    UUID id,
    String eventType,
    String traceId,
    String consumer,
    int attempts,
    String lastError
    ) {
}
