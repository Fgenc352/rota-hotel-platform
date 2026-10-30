package com.hotelbooking.reservation.application.query;

import com.hotelbooking.reservation.application.model.BookingPage;
import com.hotelbooking.reservation.application.model.BookingSearch;
import com.hotelbooking.reservation.application.model.BookingSort;
import com.hotelbooking.reservation.application.port.BookingQueries;
import com.hotelbooking.shared.application.SortDirection;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListBookingsHandler {
    public record Input(
        UUID hotel,
        int page,
        int size,
        BookingSort sort,
        SortDirection direction,
        BookingSearch search
        ) {
    }

    private final BookingQueries repository;
    public BookingPage handle(Input input) {
        return repository.bookings(input.hotel(), input.page(), input.size(), input.sort(), input.direction(),
            input.search());
    }
}
