package com.hotelbooking.guest.application.query;

import com.hotelbooking.guest.application.model.GuestSort;
import com.hotelbooking.shared.application.SortDirection;
import com.hotelbooking.guest.application.model.GuestSearch;
import com.hotelbooking.guest.application.model.GuestPage;
import com.hotelbooking.guest.application.port.GuestQueries;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchGuestsHandler {
    public record Input(UUID hotel, String q, int page, int size, String segment, GuestSearch filters, GuestSort sort,
        SortDirection direction) {
    }

    private final GuestQueries repository;

    public GuestPage handle(Input input) {
        return repository.guests(input.hotel(), input.q(), input.page(), input.size(), input.segment(), input.filters(),
            input.sort(), input.direction());
    }
}
