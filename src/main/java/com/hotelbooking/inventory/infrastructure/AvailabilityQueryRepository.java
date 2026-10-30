package com.hotelbooking.inventory.infrastructure;

import com.hotelbooking.inventory.application.port.AvailabilityQueries;
import com.hotelbooking.inventory.domain.Room;
import com.hotelbooking.inventory.infrastructure.jpa.RoomTypeJpaRepository;
import com.hotelbooking.inventory.infrastructure.mapper.RoomEntityMapper;
import com.hotelbooking.shared.domain.Stay;
import com.hotelbooking.shared.persistence.JpaQueries;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityQueryRepository implements AvailabilityQueries {
    private final RoomTypeJpaRepository rooms;
    private final RoomEntityMapper mapper;
    private final JpaQueries projections;
    private final EntityManager entityManager;

    public long version(UUID hotel) {
        return projections.count("SELECT coalesce(sum(version),0) FROM reservation WHERE hotel_id=:hotel",
            Map.of("hotel", hotel));
    }
    public List<Room> rooms(UUID hotel, Stay stay) {
        return rooms.availableTypes(hotel, stay.people(), stay.nights()).stream().map(mapper::toDomain).toList();
    }
    public int sold(UUID hotel, UUID room, Stay stay) {
        return entityManager.createQuery("""
            select coalesce(max(i.sold),0) from RoomInventoryEntity i
            where i.id.hotelId=:hotel and i.id.roomTypeId=:room and i.id.stayDate>=:start and i.id.stayDate<:end
            """,
            Integer.class).setParameter("hotel", hotel).setParameter("room", room)
            .setParameter("start", stay.checkIn()).setParameter("end", stay.checkOut()).getSingleResult();
    }
}
