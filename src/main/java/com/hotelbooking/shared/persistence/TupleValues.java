package com.hotelbooking.shared.persistence;

import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Hibernate native read-model values; no ResultSet or JDBC mapper crosses the adapter boundary. */
public final class TupleValues {
    private TupleValues() {
    }
    public static String text(Tuple row, String field) {
        return (String) row.get(field);
    }
    public static UUID uuid(Tuple row, String field) {
        return (UUID) row.get(field);
    }
    public static long number(Tuple row, String field) {
        return ((Number) row.get(field)).longValue();
    }
    public static boolean bool(Tuple row, String field) {
        return (Boolean) row.get(field);
    }
    public static BigDecimal money(Tuple row, String field) {
        return (BigDecimal) row.get(field);
    }
    public static LocalDate date(Tuple row, String field) {
        Object value = row.get(field);
        return value instanceof java.sql.Date date ? date.toLocalDate() : (LocalDate) value;
    }
    public static Instant instant(Tuple row, String field) {
        Object value = row.get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp stamp) {
            return stamp.toInstant();
        }
        return ((OffsetDateTime) value).toInstant();
    }
}
