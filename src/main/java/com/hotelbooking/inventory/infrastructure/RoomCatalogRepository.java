package com.hotelbooking.inventory.infrastructure;

import com.hotelbooking.inventory.application.model.RoomDetails;
import com.hotelbooking.inventory.application.port.RoomCatalog;
import com.hotelbooking.inventory.infrastructure.jpa.RoomTypeJpaRepository;
import com.hotelbooking.inventory.infrastructure.mapper.RoomDetailsMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomCatalogRepository implements RoomCatalog {
    private final RoomTypeJpaRepository rooms;
    private final RoomDetailsMapper mapper;

    public List<RoomDetails> rooms(UUID hotel) {
        return rooms.findByHotelIdOrderByNightlyPriceAscIdAsc(hotel).stream().map(mapper::toModel).toList();
    }
}
