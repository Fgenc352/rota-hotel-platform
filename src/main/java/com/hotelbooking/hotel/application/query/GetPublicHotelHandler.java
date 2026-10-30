package com.hotelbooking.hotel.application.query;

import com.hotelbooking.hotel.application.mapper.PublicHotelMapper;
import com.hotelbooking.hotel.application.port.HotelCatalog;
import com.hotelbooking.hotel.config.BookingProperties;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPublicHotelHandler {
    public record Input(String key) {
    }

    public record Result(
        String key,
        String name,
        String location,
        String timezone,
        BigDecimal eurRate,
        String eurRateNotice,
        String privacyVersion
        ) {
    }

    private final HotelCatalog hotels;

    private final BookingProperties properties;
    private final PublicHotelMapper publicHotelMapper;

    public Result handle(Input input) {
        var hotel = hotels.byKey(input.key());
        return publicHotelMapper.toResult(hotel, properties.eurRate());
    }
}
