package com.hotelbooking.guest.infrastructure.mapper;

import com.hotelbooking.guest.application.model.GuestSummary;
import com.hotelbooking.shared.persistence.TupleValues;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Component;

@Component
public class GuestSummaryMapper {
    public GuestSummary toModel(Tuple row) {
        return new GuestSummary(
            TupleValues.uuid(row, "id"),
            TupleValues.uuid(row, "hotel_id"),
            TupleValues.text(row, "first_name"),
            TupleValues.text(row, "last_name"),
            TupleValues.text(row, "email"),
            TupleValues.text(row, "phone"),
            TupleValues.bool(row, "marketing_allowed"),
            TupleValues.instant(row, "consent_at"),
            TupleValues.number(row, "stays"),
            TupleValues.money(row, "spend"),
            TupleValues.number(row, "active_reservations"),
            TupleValues.money(row, "booked_value")
            );
    }
}
