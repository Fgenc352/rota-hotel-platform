package com.hotelbooking.reservation.api.dto;

import com.hotelbooking.reservation.domain.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record TransitionRequest( @NotNull ReservationStatus status) {
}
