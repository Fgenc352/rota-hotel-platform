package com.hotelbooking.reservation.application.command;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record BookingData(
    UUID roomTypeId,
    LocalDate checkIn,
    LocalDate checkOut,
    int adults,
    int children,
    String firstName,
    String lastName,
    String email,
    String phone,
    boolean privacyAccepted,
    boolean marketingAllowed,
    UUID sessionId,
    List<String> serviceCodes
    ) {
    public BookingData {
        serviceCodes = serviceCodes == null?List.of():Collections.unmodifiableList(new ArrayList<>(serviceCodes));
    }

    public BookingData(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int adults, int children,
        String firstName, String lastName, String email, String phone, boolean privacyAccepted,
        boolean marketingAllowed, UUID sessionId) {
        this(roomTypeId, checkIn, checkOut, adults, children, firstName, lastName, email, phone, privacyAccepted,
            marketingAllowed, sessionId, List.of());
    }

    public BookingData normalized() {
        return new BookingData(roomTypeId, checkIn, checkOut, adults, children, firstName.strip(),
            lastName.strip(), email.strip().toLowerCase(Locale.ROOT), phone, privacyAccepted, marketingAllowed,
            sessionId, serviceCodes.stream().sorted().toList());
    }
}
