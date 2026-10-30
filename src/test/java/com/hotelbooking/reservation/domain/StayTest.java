package com.hotelbooking.reservation.domain;

import com.hotelbooking.shared.domain.Stay;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@Tag("unit")
class StayTest {
    private final LocalDate today = LocalDate.of(2026, 10, 24);

    @Test
    void departureNightIsNotConsumed() {
        var stay = new Stay(today, today.plusDays(2), 2, 1);
        stay.validate(today);
        assertThat(stay.dates()).containsExactly(today, today.plusDays(1));
        assertThat(stay.nights()).isEqualTo(2);
        assertThat(stay.people()).isEqualTo(3);
    }
}
