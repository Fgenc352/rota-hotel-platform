package com.hotelbooking.guest.application.command;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.guest.application.port.GuestProjection;
import com.hotelbooking.shared.logging.TransactionLog;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplyReservationProjectionHandler {
    public record Input(ReservationSnapshot snapshot) {
    }

    private final GuestProjection repository;

    @Transactional
    public void handle(Input input) {
        var snapshot = input.snapshot();
        // Serialize profile identity resolution within one hotel only.
        repository.lockHotel(snapshot.hotelId());
        var existing = repository.applied(snapshot.hotelId(), snapshot.id());
        UUID guest;
        if (existing.isPresent()) {
            var applied = existing.get();
            if (applied.version() >= snapshot.version()) {
                log.debug("event=crm.event_duplicate hotelId={} reservationId={} version={}", snapshot.hotelId(),
                    snapshot.id(), snapshot.version());
                return;
            }
            if (applied.version() != snapshot.version() - 1) {
                throw new IllegalStateException("Event version gap");
            }
            guest = applied.guest();
        } else {
            if (snapshot.version() != 1) {
                throw new IllegalStateException("Missing creation event");
            }
            var contacts = repository.findContactCandidates(snapshot.hotelId(), snapshot.email(), snapshot.phone());
            if (contacts.isEmpty()) {
                guest = UUID.randomUUID();
                repository.insertGuest(guest, snapshot);
            } else {
                guest = contacts.getFirst();
                for (var other : contacts.subList(1, contacts.size())) {
                    repository.mergeGuests(snapshot.hotelId(), other, guest);
                }
            }
            repository.registerIdentity(guest, snapshot);
            // Only creation events update consent; the latest explicit consent wins.
            repository.updateConsent(guest, snapshot);
        }
        repository.upsertReservation(guest, snapshot);
        TransactionLog.afterCommit(() -> log.info("event=crm.projection_updated hotelId={} reservationId={} guestId={} version={}",
            snapshot.hotelId(), snapshot.id(), guest, snapshot.version()));
    }
}
