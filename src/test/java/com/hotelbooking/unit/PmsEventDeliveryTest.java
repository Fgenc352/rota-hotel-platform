package com.hotelbooking.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.events.application.PmsGateway;
import com.hotelbooking.events.application.mapper.PmsMessageMapper;
import com.hotelbooking.events.infrastructure.PmsEventConsumer;
import com.hotelbooking.shared.json.Json;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class PmsEventDeliveryTest {
    private final ReservationSnapshot snapshot = new ReservationSnapshot(UUID.randomUUID(), UUID.randomUUID(),
        "RT-test", LocalDate.of(2026, 11, 7),
        LocalDate.of(2026, 11, 9), "Private", "Guest", "private@example.com", "+905551111111", false, Instant.now(),
        "CHECKED_OUT", BigDecimal.TEN, 3, Instant.now());

    @Test
    void createdCancelledAndChangedEventsAllReachThePmsWithTheirStableEventId() {
        var gateway = mock(PmsGateway.class);
        var consumer = new PmsEventConsumer(gateway);
        for (String type:new String[] {
            "ReservationCreated", "ReservationStatusChanged", "ReservationCancelled"
        }) {
            UUID id = UUID.randomUUID();
            consumer.accept(id, type, "trace", snapshot);
            verify(gateway).deliver(id, "trace", snapshot);
        }
        verifyNoMoreInteractions(gateway);
    }

    @Test
    void pmsMessageContainsSourceVersionStatusAndCheckoutButNoGuestContactDetails() {
        UUID event = UUID.randomUUID();
        var message = new PmsMessageMapper().toMessage(event, snapshot);
        assertThat(message.status()).isEqualTo("CHECKED_OUT");
        assertThat(message.version()).isEqualTo(3);
        assertThat(message.checkedOutAt()).isEqualTo(snapshot.checkedOutAt());
        assertThat(message.eventId()).isEqualTo(event);
        String json = new Json(new ObjectMapper().findAndRegisterModules()).write(message);
        assertThat(json).doesNotContain("private@example.com", "+905551111111", "firstName", "lastName");
    }
}
