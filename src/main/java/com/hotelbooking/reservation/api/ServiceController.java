package com.hotelbooking.reservation.api;

import com.hotelbooking.reservation.application.query.ListServicesHandler;
import com.hotelbooking.reservation.domain.ServiceOption;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/hotels/{key}/services")
@RequiredArgsConstructor
public class ServiceController {
    private final ListServicesHandler services;
    @GetMapping
    public List<ServiceOption> list(@PathVariable String key) {
        return services.handle(key);
    }
}
