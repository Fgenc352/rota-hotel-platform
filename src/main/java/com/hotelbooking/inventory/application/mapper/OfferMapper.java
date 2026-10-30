package com.hotelbooking.inventory.application.mapper;

import com.hotelbooking.inventory.application.model.Offer;
import com.hotelbooking.inventory.domain.Room;
import com.hotelbooking.shared.domain.Stay;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfferMapper {
    private final NightMapper nightMapper;

    public Offer toOffer(Room room, Stay stay, int sold) {
        var nights = stay.dates().stream().map(date -> nightMapper.toNight(room, date)).toList();
        return new Offer(
            room.id(), room.name(), room.capacity(), room.rooms() - sold, room.price(stay.checkIn()),
            room.total(stay), "TRY", nights, room.minNights(), room.nightlyPrice(), room.weekendMultiplier());
    }
}
