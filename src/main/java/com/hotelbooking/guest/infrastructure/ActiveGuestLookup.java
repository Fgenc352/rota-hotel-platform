package com.hotelbooking.guest.infrastructure;

import com.hotelbooking.guest.infrastructure.entity.GuestEntity;
import com.hotelbooking.guest.infrastructure.jpa.GuestJpaRepository;
import com.hotelbooking.shared.exception.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveGuestLookup {
    private final GuestJpaRepository guests;

    public GuestEntity get(UUID hotel, UUID id) {
        var guest = guests.findByIdAndHotelId(id, hotel).orElseThrow(BusinessException::missing);
        if (guest.getMergedInto() != null) {
            throw BusinessException.conflict("GUEST_MERGED", "Bu profil daha önce eşleştirilmiş.");
        }
        return guest;
    }
}
