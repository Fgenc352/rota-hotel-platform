package com.hotelbooking.events.application.model;

import java.time.Instant;

public record RetrySchedule(int attempts, boolean dead, Instant nextAttempt) {
}
