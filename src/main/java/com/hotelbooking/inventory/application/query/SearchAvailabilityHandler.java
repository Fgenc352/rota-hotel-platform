package com.hotelbooking.inventory.application.query;

import com.hotelbooking.hotel.application.port.HotelCatalog;
import com.hotelbooking.inventory.application.mapper.OfferMapper;
import com.hotelbooking.inventory.application.model.Offer;
import com.hotelbooking.inventory.application.port.AvailabilityQueries;
import com.hotelbooking.inventory.application.port.AvailabilityResults;
import com.hotelbooking.shared.domain.Stay;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchAvailabilityHandler {
    public record Input(String hotelKey, LocalDate checkIn, LocalDate checkOut, int adults, int children) {
    }

    private final HotelCatalog hotels;
    private final AvailabilityQueries repository;
    private final AvailabilityResults cache;
    private final OfferMapper offerMapper;

    public List<Offer> handle(Input input) {
        var hotel = hotels.byKey(input.hotelKey());
        var stay = new Stay(input.checkIn(), input.checkOut(), input.adults(), input.children());
        stay.validateForBooking(LocalDate.now(ZoneId.of(hotel.timezone())));
        long version = repository.version(hotel.id());
        var cached = cache.get(hotel.id(), stay, version);
        if (cached != null) {
            log.debug("event=availability.cache_hit hotelId={} offers={}", hotel.id(), cached.size());
            return cached;
        }
        var offers = repository.rooms(hotel.id(), stay).stream()
            .map(room -> offerMapper.toOffer(room, stay, repository.sold(hotel.id(), room.id(), stay)))
            .filter(offer -> offer.remaining() > 0)
            .toList();
        cache.put(hotel.id(), stay, version, offers);
        log.info("event=availability.searched hotelId={} offers={}", hotel.id(), offers.size());
        return offers;
    }
}
