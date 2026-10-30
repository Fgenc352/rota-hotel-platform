package com.hotelbooking.inventory.application.port;

import com.hotelbooking.inventory.application.model.Offer;
import com.hotelbooking.shared.domain.Stay;
import java.util.List;
import java.util.UUID;

public interface AvailabilityResults {
    List<Offer> get(UUID hotel, Stay stay, long version);

    void put(UUID hotel, Stay stay, long version, List<Offer> offers);
}
