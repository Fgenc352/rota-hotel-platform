package com.hotelbooking.inventory.api;

import com.hotelbooking.inventory.application.model.Offer;
import com.hotelbooking.inventory.application.query.SearchAvailabilityHandler;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/hotels/{key}/availability")
@RequiredArgsConstructor
public class AvailabilityController {
    private final SearchAvailabilityHandler availability;

    @GetMapping
    List<Offer> search(@PathVariable String key, @RequestParam LocalDate checkIn, @RequestParam LocalDate checkOut,
        @RequestParam int adults, @RequestParam(defaultValue = "0") int children) {
        return availability.handle(new SearchAvailabilityHandler.Input(key, checkIn, checkOut, adults, children));
    }
}
