package com.hotelbooking.reservation.application.model;

import java.util.List;

public record BookingPage(List<BookingSummary> items, long total) {
}
