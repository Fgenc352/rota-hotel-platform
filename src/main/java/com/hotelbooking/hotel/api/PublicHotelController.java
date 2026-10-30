package com.hotelbooking.hotel.api;

import com.hotelbooking.hotel.application.query.GetPublicHotelHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/hotels/{key}")
@RequiredArgsConstructor
public class PublicHotelController {
    private final GetPublicHotelHandler hotel;

    @GetMapping
    GetPublicHotelHandler.Result hotel(@PathVariable String key) {
        return hotel.handle(new GetPublicHotelHandler.Input(key));
    }
}
