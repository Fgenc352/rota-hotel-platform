package com.hotelbooking.guest.application.model;

import java.util.UUID;

public record AppliedReservation(UUID guest, int version) {
}
