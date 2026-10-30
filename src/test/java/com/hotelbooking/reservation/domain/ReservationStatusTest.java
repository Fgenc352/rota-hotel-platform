package com.hotelbooking.reservation.domain;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.hotelbooking.reservation.domain.ReservationStatus.*;
import static org.assertj.core.api.Assertions.*;

@Tag("unit")
class ReservationStatusTest {

    @Test
    void explicitTransitionsOnly() {
        assertThat(PENDING.canTransitionTo(CONFIRMED)).isTrue();
        assertThat(CONFIRMED.canTransitionTo(CHECKED_IN)).isTrue();
        assertThat(CHECKED_IN.canTransitionTo(CHECKED_OUT)).isTrue();
        assertThat(CONFIRMED.canTransitionTo(CANCELLED)).isTrue();
        assertThat(CONFIRMED.canTransitionTo(NO_SHOW)).isTrue();
        assertThat(CONFIRMED.canTransitionTo(CHECKED_OUT)).isFalse();
        for (var terminal:new ReservationStatus[] {
            CHECKED_OUT, CANCELLED, NO_SHOW
        }
            ) for (var next:values()) assertThat(terminal.canTransitionTo(next)).isFalse();
    }

    @Test
    void onlyCancellationAndNoShowReleaseInventory() {
        for (var status:values()) assertThat(status.releasesInventory()).isEqualTo(status == CANCELLED || status == NO_SHOW);
    }
}
