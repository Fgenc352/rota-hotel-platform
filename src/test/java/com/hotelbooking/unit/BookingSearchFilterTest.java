package com.hotelbooking.unit;

import com.hotelbooking.reservation.application.model.BookingSearch;
import com.hotelbooking.reservation.domain.ReservationStatus;
import com.hotelbooking.reservation.infrastructure.BookingSearchFilter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

@Tag("unit")
class BookingSearchFilterTest {
    @Test void combinesFiltersWithBoundValuesAndEscapesWildcards() {
        var parameters = new HashMap<String, Object>();
        var date = LocalDate.of(2027, 2, 10);
        var search = new BookingSearch("RT-%_!", " Deniz ", null, null, null, null, ReservationStatus.CHECKED_IN,
            date, date.plusDays(3), null, null, 2, 0, new BigDecimal("100"), new BigDecimal("9000"));
        var sql = new BookingSearchFilter().condition(search, parameters);
        assertThat(sql).contains("r.check_in>=:checkInFrom", "r.check_in<=:checkInTo", "r.children=:children",
            "r.status=:status", "r.total<=:maxTotal");
        assertThat(sql).doesNotContain("Deniz", "RT-%_!");
        assertThat(parameters).containsEntry("code", "%rt-!%!_!!%").containsEntry("firstName", "%deniz%")
            .containsEntry("status", "CHECKED_IN").containsEntry("children", 0);
    }
    @Test void invertedDateAndMoneyRangesAreRejectedButEmptySearchIsValid() {
        assertThat(BookingSearch.empty().isRangeValid()).isTrue();
        var date = LocalDate.of(2027, 2, 10);
        assertThat(new BookingSearch(null, null, null, null, null, null, null, date.plusDays(1), date, null,
            null, null, null, null, null).isRangeValid()).isFalse();
        assertThat(new BookingSearch(null, null, null, null, null, null, null, null, null, null, null, null,
            null, BigDecimal.TEN, BigDecimal.ONE).isRangeValid()).isFalse();
    }
}
