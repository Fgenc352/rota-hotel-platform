package com.hotelbooking.guest.infrastructure.mapper;

import com.hotelbooking.guest.infrastructure.entity.GuestMatchAuditEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GuestMatchAuditMapper {
    public GuestMatchAuditEntity toEntity(UUID hotel, UUID source, UUID target, String actor,
        String before, String after) {
        return GuestMatchAuditEntity.builder().id(UUID.randomUUID()).hotelId(hotel)
            .sourceGuestId(source).targetGuestId(target).resolvedBy(actor).resolvedAt(Instant.now())
            .snapshotVersion(1).beforeState(before).afterState(after).build();
    }
}
