package com.hotelbooking.reservation.infrastructure.mapper;

import com.hotelbooking.reservation.application.model.BookingSummary;
import com.hotelbooking.reservation.domain.ServiceLine;
import com.hotelbooking.shared.json.Json;
import com.hotelbooking.shared.persistence.TupleValues;
import jakarta.persistence.Tuple;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
@lombok.RequiredArgsConstructor
public class BookingSummaryMapper {
    private final Json json;
    public BookingSummary toModel(Tuple row) {
        return new BookingSummary( TupleValues.uuid(row, "id"), TupleValues.text(row, "code"),
            TupleValues.text(row, "first_name"), TupleValues.text(row, "last_name"), TupleValues.date(row,
            "check_in"), TupleValues.date(row, "check_out"), TupleValues.text(row, "status"),
            TupleValues.money(row, "total"), TupleValues.text(row, "room_name"), TupleValues.uuid(row,
            "guest_id"), TupleValues.uuid(row, "room_type_id"), TupleValues.text(row, "email"),
            TupleValues.text(row, "phone"), (int)TupleValues.number(row, "adults"),
            (int)TupleValues.number(row, "children"), TupleValues.text(row, "currency"),
            TupleValues.money(row, "room_total"), TupleValues.instant(row, "created_at"),
            TupleValues.instant(row, "checked_out_at"), List.of(json.read(TupleValues.text(row, "services"),
            ServiceLine[].class)));
    }
}
