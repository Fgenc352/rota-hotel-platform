package com.hotelbooking.reservation.application.query;

import com.hotelbooking.hotel.application.port.HotelCatalog;
import com.hotelbooking.reservation.application.port.ServiceCatalog;
import com.hotelbooking.reservation.domain.ServiceOption;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListServicesHandler {
    private final HotelCatalog hotels;
    private final ServiceCatalog services;
    public List<ServiceOption> handle(String hotelKey) {
        return services.services(hotels.byKey(hotelKey).id());
    }
}
