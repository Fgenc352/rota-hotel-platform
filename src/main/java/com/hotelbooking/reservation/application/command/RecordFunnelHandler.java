package com.hotelbooking.reservation.application.command;

import com.hotelbooking.hotel.application.port.HotelCatalog;
import com.hotelbooking.reservation.application.port.FunnelEvents;
import com.hotelbooking.shared.exception.BusinessException;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordFunnelHandler {
    public record Input(String hotelKey, UUID sessionId, String stage) {
    }

    private final FunnelEvents repository;
    private final HotelCatalog hotels;

    @Transactional
    public void handle(Input input) {
        if (!Set.of("SEARCH", "ROOM_SELECTED", "FORM").contains(input.stage())) {
            throw BusinessException.invalid("Onay olayı yalnızca sunucu tarafından kaydedilir.");
        }
        repository.record(hotels.byKey(input.hotelKey()).id(), input.sessionId(), input.stage());
    }
}
