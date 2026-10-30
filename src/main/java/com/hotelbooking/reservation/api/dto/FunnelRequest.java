package com.hotelbooking.reservation.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record FunnelRequest(
    @NotNull UUID sessionId,
    @NotBlank @Pattern(regexp = "SEARCH|ROOM_SELECTED|FORM|CONFIRMED"
    ) String stage) {
}
