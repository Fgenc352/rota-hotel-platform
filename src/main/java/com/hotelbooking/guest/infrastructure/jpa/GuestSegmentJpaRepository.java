package com.hotelbooking.guest.infrastructure.jpa;

import com.hotelbooking.guest.infrastructure.entity.GuestSegmentEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestSegmentJpaRepository extends JpaRepository<GuestSegmentEntity, UUID> {
    List<GuestSegmentEntity> findByHotelIdOrderByNameAscIdAsc(UUID hotel);
    Optional<GuestSegmentEntity> findByIdAndHotelId(UUID id, UUID hotel);
}
