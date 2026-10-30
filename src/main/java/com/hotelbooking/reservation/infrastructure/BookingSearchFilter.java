package com.hotelbooking.reservation.infrastructure;

import com.hotelbooking.reservation.application.model.BookingSearch;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BookingSearchFilter {
    public String condition(BookingSearch search, Map<String, Object> parameters) {
        var sql = new StringBuilder();
        text(sql, parameters, "code", "code", search.code());
        text(sql, parameters, "first_name", "firstName", search.firstName());
        text(sql, parameters, "last_name", "lastName", search.lastName());
        text(sql, parameters, "email", "email", search.email());
        text(sql, parameters, "phone", "phone", search.phone());
        value(sql, parameters, "room_type_id", "=", "roomTypeId", search.roomTypeId());
        value(sql, parameters, "status", "=", "status",
            search.status() == null?null:search.status().name());
        value(sql, parameters, "check_in", ">=", "checkInFrom", search.checkInFrom());
        value(sql, parameters, "check_in", "<=", "checkInTo", search.checkInTo());
        value(sql, parameters, "check_out", ">=", "checkOutFrom", search.checkOutFrom());
        value(sql, parameters, "check_out", "<=", "checkOutTo", search.checkOutTo());
        value(sql, parameters, "adults", "=", "adults", search.adults());
        value(sql, parameters, "children", "=", "children", search.children());
        value(sql, parameters, "total", ">=", "minTotal", search.minTotal());
        value(sql, parameters, "total", "<=", "maxTotal", search.maxTotal());
        return sql.toString();
    }

    private void text(StringBuilder sql, Map<String, Object> parameters, String column, String key,
        String value) {
        if (value == null || value.isBlank()) return;
        sql.append(" AND lower(r.").append(column).append(") LIKE :").append(key).append(" ESCAPE '!'");
        parameters.put(key, "%"+value.strip().toLowerCase(Locale.ROOT).replace("!",
            "!!").replace("%", "!%").replace("_",
            "!_")+"%");
    }

    private void value(StringBuilder sql, Map<String, Object> parameters, String column, String operator,
        String key, Object value) {
        if (value == null) return;
        sql.append(" AND r.").append(column).append(operator).append(":").append(key);
        parameters.put(key, value);
    }
}
