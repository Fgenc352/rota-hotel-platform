package com.hotelbooking.unit;

import com.hotelbooking.guest.application.model.GuestSearch;
import com.hotelbooking.guest.infrastructure.GuestSearchFilter;
import com.hotelbooking.shared.exception.BusinessException;
import java.time.LocalDate;
import java.util.HashMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

@Tag("unit")
class GuestSearchFilterTest {
    @Test
    void independentFieldsAreBoundAndWildcardsAreLiteral() {
        var values = new HashMap<String, Object>();
        String sql = new GuestSearchFilter().condition(new GuestSearch(" A%_! ", "Surname", "x@y", "+90555",
            null, null, null, null), values);
        assertThat(values).containsEntry("firstName", "%a!%!_!!%").containsEntry("lastName", "%surname%")
            .containsEntry("email", "%x@y%").containsEntry("phone", "%+90555%");
        assertThat(sql).contains("g.merged_into IS NULL").doesNotContain("A%_!");
    }

    @Test
    void allDatesMustMatchOneReservationAndBoundariesAreInclusive() {
        var day = LocalDate.of(2027, 1, 1);
        var values = new HashMap<String, Object>();
        String sql = new GuestSearchFilter().condition(new GuestSearch("", "", "", "", day, day.plusDays(1),
            day.plusDays(2), day.plusDays(3)), values);
        assertThat(sql).containsOnlyOnce("EXISTS").contains("r.check_in>=:checkInFrom", "r.check_in<=:checkInTo",
            "r.check_out>=:checkOutFrom", "r.check_out<=:checkOutTo", "r.hotel_id=g.hotel_id");
        assertThat(values).hasSize(4);
    }

    @Test
    void invalidDateRangeIsRejected() {
        var day = LocalDate.of(2027, 1, 1);
        assertThatThrownBy(() -> new GuestSearch("", "", "", "", day.plusDays(1), day, null, null))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> new GuestSearch("", "", "", "", null, null, day.plusDays(1), day))
            .isInstanceOf(BusinessException.class);
    }
}
