package com.hotelbooking.reservation.domain;

import com.hotelbooking.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@Tag("unit")
class ReservationStateTest {
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 11, 1);

    private ReservationState state(ReservationStatus status) {
        return new ReservationState("RT-TEST", status, new BigDecimal("1000.00"), CHECK_IN, CHECK_IN.plusDays(2));
    }

    @Test
    void checkInAndNoShowRequireArrivalDate() {
        var reservation = state(ReservationStatus.CONFIRMED);
        for (var next : new ReservationStatus[] {
            ReservationStatus.CHECKED_IN, ReservationStatus.NO_SHOW
        }) {
            assertThatThrownBy(() -> reservation.validateTransition(next, CHECK_IN.minusDays(1)))
                .isInstanceOf(BusinessException.class);
            assertThatCode(() -> reservation.validateTransition(next, CHECK_IN)).doesNotThrowAnyException();
        }
    }

    @Test
    void cancellationIsAllowedBeforeArrivalButCannotBeReversed() {
        assertThatCode(() -> state(ReservationStatus.CONFIRMED)
            .validateTransition(ReservationStatus.CANCELLED, CHECK_IN.minusDays(10))).doesNotThrowAnyException();
        assertThatThrownBy(() -> state(ReservationStatus.CANCELLED)
            .validateTransition(ReservationStatus.CONFIRMED, CHECK_IN)).isInstanceOf(BusinessException.class);
    }

    @Test
    void repeatedStatusIsAcceptedWithoutApplyingTransitionRulesAgain() {
        assertThatCode(() -> state(ReservationStatus.CANCELLED)
            .validateTransition(ReservationStatus.CANCELLED, CHECK_IN.minusDays(10))).doesNotThrowAnyException();
    }
}
