package com.hotelbooking.reservation.application.model;

public record IdempotencyEntry(String hash, String response) {
}
