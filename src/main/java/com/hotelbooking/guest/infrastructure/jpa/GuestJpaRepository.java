package com.hotelbooking.guest.infrastructure.jpa;

import com.hotelbooking.guest.infrastructure.entity.GuestEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestJpaRepository extends JpaRepository<GuestEntity, UUID> {
    boolean existsByIdAndHotelId(UUID id, UUID hotel);
    Optional<GuestEntity> findByIdAndHotelId(UUID id, UUID hotel);
}
