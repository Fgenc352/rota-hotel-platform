package com.hotelbooking.reservation.application.port;

import com.hotelbooking.reservation.domain.ServiceOption;
import java.util.List;
import java.util.UUID;

public interface ServiceCatalog {
    List<ServiceOption> services(UUID hotel);
}
