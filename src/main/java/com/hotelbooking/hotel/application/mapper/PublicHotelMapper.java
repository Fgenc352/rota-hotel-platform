package com.hotelbooking.hotel.application.mapper;

import com.hotelbooking.hotel.application.query.GetPublicHotelHandler;
import com.hotelbooking.hotel.domain.Hotel;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PublicHotelMapper {
    public GetPublicHotelHandler.Result toResult(Hotel hotel, BigDecimal eurRate) {
        return new GetPublicHotelHandler.Result(
            hotel.key(), hotel.name(), hotel.location(), hotel.timezone(), eurRate,
            "Demo kurudur; ödeme TRY ve oteldedir.", "privacy-v1");
    }
}
