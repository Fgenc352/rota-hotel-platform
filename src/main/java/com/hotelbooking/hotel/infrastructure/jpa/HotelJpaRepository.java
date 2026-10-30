package com.hotelbooking.hotel.infrastructure.jpa;

import com.hotelbooking.hotel.infrastructure.entity.HotelEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HotelJpaRepository extends JpaRepository<HotelEntity, UUID> {
    Optional<HotelEntity> findByPublicKey(String key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HotelEntity h where h.id=:id")
    Optional<HotelEntity> lockById(@Param("id") UUID id);
}
