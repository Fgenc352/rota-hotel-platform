package com.hotelbooking.reservation.application.port;

import com.hotelbooking.reservation.application.model.BookingPage;
import com.hotelbooking.reservation.application.model.BookingSearch;
import com.hotelbooking.reservation.application.model.BookingSort;
import com.hotelbooking.reservation.application.model.BookingSummary;
import com.hotelbooking.shared.application.SortDirection;
import java.util.UUID;

public interface BookingQueries {
    BookingPage bookings(UUID hotel, int page, int size, BookingSort sort, SortDirection direction,
        BookingSearch search);
    java.util.List<BookingSummary> daily(UUID hotel, java.time.LocalDate date);
    BookingSummary detail(UUID hotel, UUID reservation);
}
