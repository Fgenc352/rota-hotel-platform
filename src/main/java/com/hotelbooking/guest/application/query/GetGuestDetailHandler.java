package com.hotelbooking.guest.application.query;

import com.hotelbooking.guest.application.model.GuestDetail;
import com.hotelbooking.guest.application.port.GuestQueries;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetGuestDetailHandler {
    public record Input(UUID hotel, UUID id) {
    }

    private final GuestQueries repository;

    public GuestDetail handle(Input input) {
        return repository.detail(input.hotel(), input.id());
    }
}
