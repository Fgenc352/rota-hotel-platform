package com.hotelbooking.unit;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.guest.application.command.ApplyReservationProjectionHandler;
import com.hotelbooking.guest.application.model.AppliedReservation;
import com.hotelbooking.guest.application.port.GuestProjection;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ContactGuestIdentityTest {
    private final Projection projection = new Projection();
    private final ApplyReservationProjectionHandler handler = new ApplyReservationProjectionHandler(projection);
    private final UUID hotel = UUID.randomUUID();

    @Test
    void sameEmailWithDifferentPhonesReusesGuest() {
        var first = event(hotel, "same@example.com", "+905551111111");
        var second = event(hotel, "same@example.com", "+905552222222");
        apply(first);
        apply(second);
        assertThat(projection.guests).hasSize(1);
        assertThat(projection.reservations.get(first.id()).guest()).isEqualTo(projection.reservations.get(second.id()).guest());
    }

    @Test
    void differentEmailWithTheSamePhoneReusesGuest() {
        var first = event(hotel, "first@example.com", "+905551111111");
        var second = event(hotel, "changed@example.com", "+905551111111");
        apply(first);
        apply(second);
        assertThat(projection.guests).hasSize(1);
        assertThat(projection.reservations.get(first.id()).guest()).isEqualTo(projection.reservations.get(second.id()).guest());
        assertThat(projection.reservations).hasSize(2);
    }

    @Test
    void identicalEmailAndPhoneReuseTheExistingGuest() {
        var first = event(hotel, "same@example.com", "+905551111111");
        var second = event(hotel, "same@example.com", "+905551111111");
        apply(first);
        apply(second);
        assertThat(projection.guests).hasSize(1);
        assertThat(projection.reservations.get(first.id()).guest()).isEqualTo(projection.reservations.get(second.id()).guest());
        assertThat(projection.reservations).hasSize(2);
    }

    @Test
    void theSamePhoneInDifferentHotelsRemainsSeparate() {
        var first = event(hotel, "same@example.com", "+905551111111");
        var second = event(UUID.randomUUID(), "same@example.com", "+905551111111");
        apply(first);
        apply(second);
        assertThat(projection.guests).hasSize(2);
        assertThat(projection.reservations.get(first.id()).guest()).isNotEqualTo(projection.reservations.get(second.id()).guest());
    }

    @Test
    void repeatedEventDoesNotCreateAnotherProfileOrReservation() {
        var event = event(hotel, "same@example.com", "+905551111111");
        apply(event);
        apply(event);
        assertThat(projection.guests).hasSize(1);
        assertThat(projection.reservations).hasSize(1);
        assertThat(projection.updates).isEqualTo(1);
    }

    @Test
    void unrelatedContactsCreateAnotherGuest() {
        apply(event(hotel, "a@example.com", "+905551111111"));
        apply(event(hotel, "b@example.com", "+905552222222"));
        assertThat(projection.guests).hasSize(2);
        assertThat(projection.contacts).hasSize(2);
    }

    @Test
    void aContactBridgeConsolidatesAllReservationsAndAliasesIntoOneProfile() {
        var first = event(hotel, "a@example.com", "+905551111111");
        var second = event(hotel, "b@example.com", "+905552222222");
        apply(first);
        apply(second);
        assertThat(projection.guests).hasSize(2);
        apply(event(hotel, first.email(), second.phone()));
        apply(event(hotel, second.email(), "+905553333333"));
        apply(event(hotel, "new@example.com", first.phone()));
        assertThat(projection.guests).hasSize(1);
        assertThat(projection.reservations.values().stream().map(AppliedReservation::guest).distinct()).hasSize(1);
        assertThat(projection.contacts.values().stream().distinct()).hasSize(1);
        assertThat(projection.reservations).hasSize(5);
    }

    @Test
    void newlyLearnedEmailsAndPhonesAreBothReusableWithoutLosingOlderContacts() {
        apply(event(hotel, "a@example.com", "+905551111111"));
        apply(event(hotel, "b@example.com", "+905551111111"));
        apply(event(hotel, "b@example.com", "+905552222222"));
        apply(event(hotel, "c@example.com", "+905552222222"));
        apply(event(hotel, "a@example.com", "+905553333333"));
        assertThat(projection.guests).hasSize(1);
        assertThat(projection.contacts).hasSize(5);
        assertThat(projection.reservations).hasSize(5);
    }

    private void apply(ReservationSnapshot event) {
        handler.handle(new ApplyReservationProjectionHandler.Input(event));
    }

    private ReservationSnapshot event(UUID hotel, String email, String phone) {
        var day = LocalDate.of(2027, 1, 1);
        return new ReservationSnapshot(UUID.randomUUID(), hotel, "RT-test", day, day.plusDays(1), "Test", "Guest",
            email, phone, false, Instant.now(), "CONFIRMED", BigDecimal.TEN, 1, null);
    }

    private record Contact(UUID hotel, String email, String phone) {
    }

    private static class Projection implements GuestProjection {
        private final Map<Contact, UUID> contacts = new HashMap<>();
        private final Map<UUID, ReservationSnapshot> guests = new HashMap<>();
        private final Map<UUID, AppliedReservation> reservations = new HashMap<>();
        private int updates;
        public void lockHotel(UUID hotel) {
        }
        public Optional<AppliedReservation> applied(UUID hotel, UUID reservation) {
            return Optional.ofNullable(reservations.get(reservation));
        }
        public java.util.List<UUID> findContactCandidates(UUID hotel, String email, String phone) {
            return contacts.entrySet().stream().filter(entry -> entry.getKey().hotel().equals(hotel))
                .filter(entry -> entry.getKey().email().equals(email) || entry.getKey().phone().equals(phone))
                .map(Map.Entry::getValue).distinct().sorted().toList();
        }
        public void mergeGuests(UUID hotel, UUID source, UUID target) {
            contacts.replaceAll((key, owner) -> owner.equals(source) ? target : owner);
            reservations.replaceAll((id, value) -> value.guest().equals(source) ? new AppliedReservation(target, value.version()) : value);
            guests.remove(source);
        }
        public void insertGuest(UUID id, ReservationSnapshot event) {
            guests.put(id, event);
        }
        public void registerIdentity(UUID id, ReservationSnapshot event) {
            contacts.put(new Contact(event.hotelId(), event.email(), event.phone()), id);
        }
        public void updateConsent(UUID guest, ReservationSnapshot event) {
        }
        public void upsertReservation(UUID guest, ReservationSnapshot event) {
            reservations.put(event.id(), new AppliedReservation(guest, event.version()));
            updates++;
        }
    }
}
