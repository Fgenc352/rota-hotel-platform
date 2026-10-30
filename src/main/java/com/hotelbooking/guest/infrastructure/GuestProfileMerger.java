package com.hotelbooking.guest.infrastructure;

import com.hotelbooking.guest.infrastructure.mapper.GuestMatchAuditMapper;
import com.hotelbooking.hotel.application.port.HotelCatalog;
import com.hotelbooking.shared.exception.BusinessException;
import com.hotelbooking.shared.persistence.JpaQueries;
import com.hotelbooking.shared.logging.TransactionLog;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class GuestProfileMerger {
    private final HotelCatalog hotels;
    private final ActiveGuestLookup activeGuests;
    private final JpaQueries queries;
    private final EntityManager entityManager;
    private final GuestMatchSnapshotReader snapshots;
    private final GuestMatchAuditMapper auditMapper;

    @Transactional
    public void merge(UUID hotel, UUID sourceId, UUID targetId) {
        hotels.lock(hotel);
        // Same hotel lock as the async projector keeps automatic consolidation atomic.
        if (sourceId.equals(targetId)) {
            throw BusinessException.invalid("Aynı profil seçilemez.");
        }
        var prior = queries.tuples("SELECT target_guest_id FROM guest_match_audit WHERE hotel_id=:hotel AND source_guest_id=:source",
            Map.of("hotel", hotel, "source", sourceId));
        if (!prior.isEmpty()) {
            if (com.hotelbooking.shared.persistence.TupleValues.uuid(prior.getFirst(), "target_guest_id").equals(targetId)) {
                return;
            }
            throw BusinessException.conflict("MATCH_ALREADY_RESOLVED", "Bu eşleşme daha önce başka bir profile bağlandı.");
        }
        var source = activeGuests.get(hotel, sourceId);
        var target = activeGuests.get(hotel, targetId);
        entityManager.flush();
        String before = snapshots.capture(hotel, sourceId, targetId);
        var params = Map.<String, Object>of("hotel", hotel, "source", sourceId, "target", targetId);
        queries.execute("UPDATE crm_reservation SET guest_id=:target WHERE hotel_id=:hotel AND guest_id=:source",
            params);
        queries.execute("UPDATE guest_note SET guest_id=:target WHERE hotel_id=:hotel AND guest_id=:source",
            params);
        queries.execute("""
            INSERT INTO guest_tag(hotel_id,guest_id,tag)
            SELECT hotel_id,:target,tag FROM guest_tag WHERE hotel_id=:hotel AND guest_id=:source ON CONFLICT DO NOTHING
            """,
            params);
        queries.execute("DELETE FROM guest_tag WHERE hotel_id=:hotel AND guest_id=:source", params);
        queries.execute("UPDATE guest_identifier SET guest_id=:target WHERE hotel_id=:hotel AND guest_id=:source",
            params);
        // Apply consent using the same latest-explicit-choice rule as the projector.
        target.updateConsent(source.isMarketingAllowed(), source.getConsentAt());
        entityManager.flush();
        queries.execute("UPDATE guest SET merged_into=:target,matching_conflict=false WHERE hotel_id=:hotel AND id=:source",
            params);
        queries.execute("UPDATE guest SET matching_conflict=false WHERE hotel_id=:hotel AND id=:target", params);
        String after = snapshots.capture(hotel, sourceId, targetId);
        entityManager.persist(auditMapper.toEntity(hotel, sourceId, targetId, "SYSTEM_CONTACT_MATCH", before, after));
        entityManager.flush();
        entityManager.clear();
        TransactionLog.afterCommit(() -> log.info("event=crm.profiles_merged hotelId={} sourceGuestId={} targetGuestId={} reason=contact_match",
            hotel, sourceId, targetId));
    }

}
