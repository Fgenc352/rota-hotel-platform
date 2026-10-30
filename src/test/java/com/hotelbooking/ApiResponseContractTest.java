package com.hotelbooking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hotelbooking.events.application.model.FailedEvent;
import com.hotelbooking.guest.application.model.GuestPage;
import com.hotelbooking.guest.application.model.GuestSummary;
import com.hotelbooking.guest.application.model.Segment;
import com.hotelbooking.reservation.api.dto.BookingRequest;
import com.hotelbooking.reservation.application.command.BookingData;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@Tag("unit")
class ApiResponseContractTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final UUID id = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Test
    void guestPageKeepsColumnNamesAmountsAndTimestamps() throws Exception {
        var consent = Instant.parse("2026-10-26T10:00:00Z");
        var guest = new GuestSummary(id, id, "Deniz", "Test", "d@example.com", "+905551234567", true,
            consent, 2, new BigDecimal("2400.50"), 3, new BigDecimal("3600.75"));
        var page = mapper.readTree(mapper.writeValueAsString(new GuestPage(List.of(guest), 1, 0, 20)));
        assertThat(page.size()).isEqualTo(4);
        assertThat(page.path("total").asLong()).isEqualTo(1);
        assertThat(page.path("page").asInt()).isZero();
        assertThat(page.path("size").asInt()).isEqualTo(20);
        var row = page.path("items").get(0);
        assertThat(row.size()).isEqualTo(12);
        assertThat(row.path("hotel_id").asText()).isEqualTo(id.toString());
        assertThat(row.path("first_name").asText()).isEqualTo("Deniz");
        assertThat(row.path("marketing_allowed").asBoolean()).isTrue();
        assertThat(row.has("matching_conflict")).isFalse();
        assertThat(row.path("consent_at").asText()).isEqualTo(consent.toString());
        assertThat(row.path("spend").decimalValue()).isEqualByComparingTo("2400.50");
        assertThat(row.has("active_reservations")).isTrue();
        assertThat(row.has("booked_value")).isTrue();
    }

    @Test
    void nullableFieldsRemainPresentAndEventFieldsKeepTheirNames() throws Exception {
        var segment = mapper.readTree(mapper.writeValueAsString(new Segment(id, id, "VIP", 2, 12, false)));
        assertThat(segment.has("days_since_checkout")).isFalse();
        assertThat(segment.path("min_stays").asInt()).isEqualTo(2);
        var event = mapper.readTree(mapper.writeValueAsString(new FailedEvent(id, "ReservationCreated",
            "trace", "PMS", 10, null)));
        assertThat(event.path("event_type").asText()).isEqualTo("ReservationCreated");
        assertThat(event.path("trace_id").asText()).isEqualTo("trace");
        assertThat(event.has("last_error")).isTrue();
        assertThat(event.path("last_error").isNull()).isTrue();
    }

    @Test
    void bookingNormalizationKeepsTheExistingIdempotencyHashInput() throws Exception {
        var date = LocalDate.of(2026, 11, 1);
        var data = new BookingData(id, date, date.plusDays(2), 2, 0, " Deniz ", " Test ",
            " DENIZ@example.com ", "+905551234567", true, false, id).normalized();
        var previousRequest = new BookingRequest(id, date, date.plusDays(2), 2, 0, "Deniz", "Test",
            "deniz@example.com", "+905551234567", true, false, id);
        assertThat(mapper.writeValueAsString(data)).isEqualTo(mapper.writeValueAsString(previousRequest));
    }
}
