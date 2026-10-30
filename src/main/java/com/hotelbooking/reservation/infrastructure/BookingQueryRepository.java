package com.hotelbooking.reservation.infrastructure;

import com.hotelbooking.reservation.application.model.BookingPage;
import com.hotelbooking.reservation.application.model.BookingSearch;
import com.hotelbooking.reservation.application.model.BookingSort;
import com.hotelbooking.reservation.application.model.BookingSummary;
import com.hotelbooking.reservation.application.port.BookingQueries;
import com.hotelbooking.reservation.infrastructure.mapper.BookingSummaryMapper;
import com.hotelbooking.shared.application.SortDirection;
import com.hotelbooking.shared.exception.BusinessException;
import com.hotelbooking.shared.persistence.JpaQueries;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingQueryRepository implements BookingQueries {
    private static final String SELECT = """
        SELECT r.*,t.name AS room_name,c.guest_id,CAST(r.service_lines AS text) AS services
        FROM reservation r JOIN room_type t ON t.hotel_id=r.hotel_id AND t.id=r.room_type_id
        LEFT JOIN crm_reservation c ON c.hotel_id=r.hotel_id AND c.reservation_id=r.id
        """;
    private final JpaQueries projections;
    private final BookingOrdering ordering;
    private final BookingSearchFilter filters;
    private final BookingSummaryMapper mapper;
    public BookingPage bookings(UUID hotel, int page, int size, BookingSort sort, SortDirection direction,
        BookingSearch search) {
        var parameters = new HashMap<String, Object>();
        parameters.put("hotel", hotel);
        parameters.put("limit", size);
        parameters.put("offset", page * size);
        String where = " WHERE r.hotel_id=:hotel" + filters.condition(search, parameters);
        var items = projections.tuples(SELECT + where + ordering.clause(sort, direction)+" LIMIT :limit OFFSET :offset",
            parameters)
            .stream().map(mapper::toModel).toList();
        return new BookingPage(items, projections.count("SELECT count(*) FROM reservation r" + where, parameters));
    }

    public java.util.List<BookingSummary> daily(UUID hotel, java.time.LocalDate date) {
        return projections.tuples(SELECT + """
            WHERE r.hotel_id=:hotel AND r.status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT')
            AND ((r.check_in<=:ahead AND r.check_out>=:behind) OR r.status='CHECKED_IN')
            ORDER BY r.check_in,r.code
            """,
            Map.of("hotel", hotel, "ahead", date.plusDays(7), "behind", date.minusDays(1)))
            .stream().map(mapper::toModel).toList();
    }

    public BookingSummary detail(UUID hotel, UUID reservation) {
        return projections.tuples(SELECT + " WHERE r.hotel_id=:hotel AND r.id=:reservation", Map.of("hotel",
            hotel, "reservation",
            reservation))
            .stream().findFirst().map(mapper::toModel).orElseThrow(BusinessException::missing);
    }
}
