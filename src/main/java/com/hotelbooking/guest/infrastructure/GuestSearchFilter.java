package com.hotelbooking.guest.infrastructure;

import com.hotelbooking.guest.application.model.GuestSearch;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GuestSearchFilter {
    public String condition(GuestSearch search, Map<String, Object> parameters) {
        var sql = new StringBuilder(" AND g.merged_into IS NULL");
        text(sql, parameters, "first_name", "firstName", search.firstName());
        text(sql, parameters, "last_name", "lastName", search.lastName());
        text(sql, parameters, "email", "email", search.email());
        text(sql, parameters, "phone", "phone", search.phone());
        var dates = new StringBuilder();
        date(dates, parameters, "check_in", ">=", "checkInFrom", search.checkInFrom());
        date(dates, parameters, "check_in", "<=", "checkInTo", search.checkInTo());
        date(dates, parameters, "check_out", ">=", "checkOutFrom", search.checkOutFrom());
        date(dates, parameters, "check_out", "<=", "checkOutTo", search.checkOutTo());
        if (!dates.isEmpty()) {
            sql.append(" AND EXISTS (SELECT 1 FROM crm_reservation r WHERE r.hotel_id=g.hotel_id AND r.guest_id=g.id")
                .append(dates).append(")");
        }
        return sql.toString();
    }

    private void text(StringBuilder sql, Map<String, Object> parameters, String column, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (column.equals("email") || column.equals("phone")) {
            sql.append(" AND EXISTS (SELECT 1 FROM guest_identifier i WHERE i.hotel_id=g.hotel_id AND i.guest_id=g.id AND lower(i.")
                .append(column).append(") LIKE :").append(key).append(" ESCAPE '!')");
        } else {
            sql.append(" AND lower(g.").append(column).append(") LIKE :").append(key).append(" ESCAPE '!'");
        }
        parameters.put(key, "%" + value.strip().toLowerCase(Locale.ROOT)
            .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
    }

    private void date(StringBuilder sql, Map<String, Object> parameters, String column, String operator,
        String key, java.time.LocalDate value) {
        if (value == null) {
            return;
        }
        sql.append(" AND r.").append(column).append(operator).append(":").append(key);
        parameters.put(key, value);
    }
}
