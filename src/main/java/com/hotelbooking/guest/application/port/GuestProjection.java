package com.hotelbooking.guest.application.port;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.guest.application.model.AppliedReservation;
import java.util.Optional;
import java.util.UUID;

public interface GuestProjection {
    void lockHotel(UUID hotel);

    Optional<AppliedReservation> applied(UUID hotel, UUID reservation);

    java.util.List<UUID> findContactCandidates(UUID hotel, String email, String phone);

    void mergeGuests(UUID hotel, UUID source, UUID target);

    void insertGuest(UUID guest, ReservationSnapshot snapshot);

    void registerIdentity(UUID guest, ReservationSnapshot snapshot);

    void updateConsent(UUID guest, ReservationSnapshot snapshot);

    void upsertReservation(UUID guest, ReservationSnapshot snapshot);
}
