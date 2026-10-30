package com.hotelbooking.guest.application.port;

import com.hotelbooking.guest.application.model.GuestSort;
import com.hotelbooking.shared.application.SortDirection;
import com.hotelbooking.guest.application.model.GuestSearch;
import com.hotelbooking.guest.application.model.GuestDetail;
import com.hotelbooking.guest.application.model.GuestPage;
import java.util.UUID;

public interface GuestQueries {
    GuestPage guests(UUID hotel, String search, int page, int size, String segment,
        GuestSearch filters, GuestSort sort, SortDirection direction);

    GuestDetail detail(UUID hotel, UUID id);
}
