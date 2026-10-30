package com.hotelbooking.inventory.infrastructure.jpa;

import com.hotelbooking.inventory.infrastructure.entity.RoomTypeEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomTypeJpaRepository extends JpaRepository<RoomTypeEntity, UUID> {
    List<RoomTypeEntity> findByHotelIdOrderByNightlyPriceAscIdAsc(UUID hotel);
    Optional<RoomTypeEntity> findByHotelIdAndId(UUID hotelId, UUID id);

    @Query("select r from RoomTypeEntity r where r.hotelId=:hotel and r.capacity>=:people and r.minNights<=:nights order by r.nightlyPrice,r.id")
    List<RoomTypeEntity> availableTypes(@Param("hotel") UUID hotel, @Param("people") int people, @Param("nights") long nights);
}
