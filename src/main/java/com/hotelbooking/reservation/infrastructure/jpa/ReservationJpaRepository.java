package com.hotelbooking.reservation.infrastructure.jpa;

import com.hotelbooking.reservation.infrastructure.entity.ReservationEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, UUID> {
    @Query("select r.roomTypeId from ReservationEntity r where r.id=:id and r.hotelId=:hotel")
    Optional<UUID> findRoomIdByHotelAndId(@Param("hotel") UUID hotel, @Param("id") UUID id);

    Optional<ReservationEntity> findByIdAndHotelId(UUID id, UUID hotel);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservationEntity r where r.id=:id and r.hotelId=:hotel")
    Optional<ReservationEntity> lockByHotelAndId(@Param("hotel") UUID hotel, @Param("id") UUID id);
}
