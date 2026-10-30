package com.hotelbooking.reservation.infrastructure;

import com.hotelbooking.reservation.application.model.BookingSort;
import com.hotelbooking.shared.application.SortDirection;
import org.springframework.stereotype.Component;

@Component
public class BookingOrdering {
    public String clause(BookingSort field, SortDirection direction) {
        String column = switch (field) {
            case CREATED_AT -> "r.created_at";
            case CHECK_IN -> "r.check_in";
            case CHECK_OUT -> "r.check_out";
            case GUEST_NAME -> "lower(r.first_name||' '||r.last_name)";
            case ROOM_NAME -> "lower(t.name)";
            case STATUS -> "r.status";
            case TOTAL -> "r.total";
        };
        return " ORDER BY " + column + " " + direction.name() + ",r.id ASC";
    }
}
