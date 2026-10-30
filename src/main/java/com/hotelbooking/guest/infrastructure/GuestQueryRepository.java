package com.hotelbooking.guest.infrastructure;

import com.hotelbooking.guest.application.model.GuestSort;
import com.hotelbooking.shared.application.SortDirection;
import com.hotelbooking.guest.application.model.GuestSearch;
import com.hotelbooking.guest.application.model.GuestDetail;
import com.hotelbooking.guest.application.model.GuestPage;
import com.hotelbooking.guest.application.port.GuestQueries;
import com.hotelbooking.guest.infrastructure.entity.CrmReservationEntity;
import com.hotelbooking.guest.infrastructure.entity.GuestNoteEntity;
import com.hotelbooking.guest.infrastructure.mapper.CrmReservationEntityMapper;
import com.hotelbooking.guest.infrastructure.mapper.GuestNoteEntityMapper;
import com.hotelbooking.guest.infrastructure.mapper.GuestSummaryMapper;
import com.hotelbooking.guest.infrastructure.segment.GuestSegmentFilters;
import com.hotelbooking.shared.exception.BusinessException;
import com.hotelbooking.shared.persistence.JpaQueries;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestQueryRepository implements GuestQueries {
    private static final String SUMMARY = """
        SELECT g.*, coalesce(s.stays, 0) AS stays, coalesce(s.spend, 0) AS spend,
            coalesce(s.active_reservations, 0) AS active_reservations,
            coalesce(s.booked_value, 0) AS booked_value
        FROM guest g
        LEFT JOIN LATERAL (
            SELECT count(*) FILTER (WHERE status='CHECKED_OUT') AS stays,
                sum(total) FILTER (WHERE status='CHECKED_OUT') AS spend,
                count(*) FILTER (WHERE status NOT IN ('CANCELLED','NO_SHOW')) AS active_reservations,
                sum(total) FILTER (WHERE status NOT IN ('CANCELLED','NO_SHOW')) AS booked_value
            FROM crm_reservation r
            WHERE r.hotel_id=g.hotel_id AND r.guest_id=g.id
        ) s ON true
        """;

    private final GuestSearchFilter searchFilter;
    private final GuestOrdering ordering;
    private final JpaQueries projections;
    private final EntityManager entityManager;
    private final GuestSegmentFilters segments;
    private final GuestSummaryMapper summaryMapper;
    private final GuestNoteEntityMapper noteMapper;
    private final CrmReservationEntityMapper reservationMapper;

    public GuestPage guests(UUID hotel, String search, int page, int size, String segment, GuestSearch searchFilters,
        GuestSort sort, SortDirection direction) {
        String term = "%" + search.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_",
            "!_") + "%";
        var parameters = new HashMap<String, Object>();
        parameters.put("hotel", hotel);
        parameters.put("term", term);
        parameters.put("limit", size);
        parameters.put("offset", page * size);
        String filter = """
            WHERE g.hotel_id=:hotel
            AND (lower(g.first_name||' '||g.last_name||' '||g.email||' '||g.phone) LIKE :term ESCAPE '!'
                OR EXISTS (SELECT 1 FROM guest_identifier i WHERE i.hotel_id=g.hotel_id AND i.guest_id=g.id
                    AND lower(i.email||' '||i.phone) LIKE :term ESCAPE '!'))
            """ + searchFilter.condition(searchFilters,
            parameters) + " " + segments.condition(hotel, segment, parameters);
        var items = projections.tuples(SUMMARY + filter + ordering.clause(sort, direction) + " LIMIT :limit OFFSET :offset",
            parameters)
            .stream().map(summaryMapper::toModel).toList();
        return new GuestPage(items, projections.count("SELECT count(*) FROM guest g " + filter, parameters),
            page, size);
    }

    public GuestDetail detail(UUID hotel, UUID id) {
        var records = projections.tuples(SUMMARY + " WHERE g.hotel_id=:hotel AND g.id=:id", Map.of("hotel",
            hotel, "id", id));
        if (records.isEmpty()) {
            throw BusinessException.missing();
        }
        var reservations = entityManager.createQuery("""
            select r from CrmReservationEntity r where r.id.hotelId=:hotel and r.guestId=:guest
            order by r.checkIn desc,r.id.reservationId
            """,
            CrmReservationEntity.class).setParameter("hotel", hotel).setParameter("guest", id).getResultList()
            .stream().map(reservationMapper::toModel).toList();
        var notes = entityManager.createQuery("""
            select n from GuestNoteEntity n where n.hotelId=:hotel and n.guestId=:guest order by n.createdAt desc,n.id
            """,
            GuestNoteEntity.class).setParameter("hotel", hotel).setParameter("guest", id).getResultList()
            .stream().map(noteMapper::toModel).toList();
        var tags = entityManager.createQuery("""
            select t.id.tag from GuestTagEntity t where t.id.hotelId=:hotel and t.id.guestId=:guest order by t.id.tag
            """,
            String.class).setParameter("hotel", hotel).setParameter("guest", id).getResultList();
        var emails = entityManager.createQuery("""
            select distinct i.id.email from GuestIdentifierEntity i
            where i.id.hotelId=:hotel and i.guestId=:guest order by i.id.email
            """, String.class).setParameter("hotel", hotel).setParameter("guest", id).getResultList();
        var phones = entityManager.createQuery("""
            select distinct i.id.phone from GuestIdentifierEntity i
            where i.id.hotelId=:hotel and i.guestId=:guest order by i.id.phone
            """, String.class).setParameter("hotel", hotel).setParameter("guest", id).getResultList();
        return new GuestDetail(summaryMapper.toModel(records.getFirst()), reservations, notes, tags, emails, phones);
    }
}
