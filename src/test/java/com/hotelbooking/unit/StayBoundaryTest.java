package com.hotelbooking.unit;

import com.hotelbooking.shared.domain.Stay;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class StayBoundaryTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    static Stream<Stay> validStays() {
        return Stream.of(
            new Stay(TODAY, TODAY.plusDays(1), 1, 0),
            new Stay(TODAY, TODAY.plusDays(30), 10, 10),
            new Stay(TODAY.plusYears(1).minusDays(1), TODAY.plusYears(1), 2, 0));
    }

    static Stream<Stay> invalidStays() {
        return Stream.of(
            new Stay(null, TODAY.plusDays(1), 2, 0),
            new Stay(TODAY, null, 2, 0),
            new Stay(TODAY.minusDays(1), TODAY.plusDays(1), 2, 0),
            new Stay(TODAY, TODAY, 2, 0),
            new Stay(TODAY.plusDays(1), TODAY, 2, 0),
            new Stay(TODAY, TODAY.plusDays(31), 2, 0),
            new Stay(TODAY.plusYears(1), TODAY.plusYears(1).plusDays(1), 2, 0),
            new Stay(TODAY, TODAY.plusDays(1), 0, 0),
            new Stay(TODAY, TODAY.plusDays(1), 11, 0),
            new Stay(TODAY, TODAY.plusDays(1), 2, -1),
            new Stay(TODAY, TODAY.plusDays(1), 2, 11));
    }

    @ParameterizedTest(name = "accepts {0}")
    @MethodSource("validStays")
    void acceptsInclusiveLimits(Stay stay) {
        assertThatCode(() -> stay.validate(TODAY)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "rejects {0}")
    @MethodSource("invalidStays")
    void rejectsInvalidDatesAndPartySizes(Stay stay) {
        assertThatThrownBy(() -> stay.validate(TODAY)).isInstanceOf(IllegalArgumentException.class);
    }
}
