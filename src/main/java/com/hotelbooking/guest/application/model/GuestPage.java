package com.hotelbooking.guest.application.model;

import java.util.List;

public record GuestPage(List<GuestSummary> items, long total, int page, int size) {
}
