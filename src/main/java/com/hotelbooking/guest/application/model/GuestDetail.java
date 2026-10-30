package com.hotelbooking.guest.application.model;

import java.util.List;

public record GuestDetail(
    GuestSummary guest,
    List<GuestReservation> reservations,
    List<GuestNote> notes,
    List<String> tags,
    List<String> emails,
    List<String> phones
    ) {
}
