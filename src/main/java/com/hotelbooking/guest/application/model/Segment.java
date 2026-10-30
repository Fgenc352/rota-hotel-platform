package com.hotelbooking.guest.application.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Segment(
    UUID id,
    UUID hotelId,
    String name,
    int minStays,
    int months,
    boolean marketingRequired
    ) {
}
