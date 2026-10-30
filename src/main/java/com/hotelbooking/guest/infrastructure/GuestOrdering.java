package com.hotelbooking.guest.infrastructure;

import com.hotelbooking.guest.application.model.GuestSort;
import com.hotelbooking.shared.application.SortDirection;
import org.springframework.stereotype.Component;

@Component
public class GuestOrdering {
    public String clause(GuestSort field, SortDirection direction) {
        String column = switch (field) {
            case FIRST_NAME -> "lower(g.first_name)";
            case LAST_NAME -> "lower(g.last_name)";
            case EMAIL -> "lower(g.email)";
            case PHONE -> "g.phone";
        };
        return " ORDER BY " + column + " " + direction.name() + ",g.id ASC";
    }
}
