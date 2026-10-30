package com.hotelbooking.unit;

import com.hotelbooking.inventory.domain.Room;
import com.hotelbooking.shared.domain.Stay;
import com.hotelbooking.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@Tag("unit")
class RoomRulesTest {
    private final LocalDate saturday = LocalDate.of(2026, 11, 7);
    private final Room room = new Room(UUID.randomUUID(), UUID.randomUUID(), "Suite", 3, 2,
        new BigDecimal("100.01"), new BigDecimal("1.50"), 2);

    @Test
    void acceptsExactCapacityAndMinimumNights() {
        assertThatCode(() -> room.validateStay(new Stay(saturday, saturday.plusDays(2), 2, 1)))
            .doesNotThrowAnyException();
    }

    @Test
    void countsChildrenTowardsCapacityAndRejectsShortStays() {
        assertThatThrownBy(() -> room.validateStay(new Stay(saturday, saturday.plusDays(2), 2, 2)))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> room.validateStay(new Stay(saturday, saturday.plusDays(1), 2, 0)))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void roundsEachWeekendNightAndTotalsTheActualNights() {
        assertThat(room.price(saturday)).isEqualByComparingTo("150.02");
        assertThat(room.price(saturday.plusDays(2))).isEqualByComparingTo("100.01");
        assertThat(room.total(new Stay(saturday, saturday.plusDays(3), 2, 0)))
            .isEqualByComparingTo("400.05");
    }
}
