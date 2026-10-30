package com.hotelbooking.guest.infrastructure;

import com.hotelbooking.guest.application.port.GuestCommands;
import com.hotelbooking.guest.application.model.GuestNote;
import com.hotelbooking.guest.infrastructure.mapper.GuestNoteEntityMapper;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import com.hotelbooking.guest.infrastructure.entity.GuestNoteEntity;
import com.hotelbooking.guest.infrastructure.jpa.GuestJpaRepository;
import com.hotelbooking.shared.persistence.JpaQueries;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class GuestCommandRepository implements GuestCommands {
    private final EntityManager entityManager;
    private final GuestJpaRepository guests;
    private final JpaQueries queries;
    private final GuestNoteEntityMapper noteMapper;

    public boolean exists(UUID hotel, UUID id) {
        return guests.existsByIdAndHotelId(id, hotel);
    }
    public void insertNote(UUID note, UUID hotel, UUID guest, String author, String body) {
        entityManager.persist(GuestNoteEntity.builder().id(note).hotelId(hotel).guestId(guest).author(author).body(body).createdAt(Instant.now()).build());
    }
    public Optional<GuestNote> lockNote(UUID hotel, UUID guest, UUID note) {
        return entityManager.createQuery("""
            select n from GuestNoteEntity n
            where n.hotelId=:hotel and n.guestId=:guest and n.id=:note
            """, GuestNoteEntity.class)
            .setParameter("hotel", hotel).setParameter("guest", guest).setParameter("note", note)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList().stream().findFirst().map(noteMapper::toModel);
    }

    public void deleteNote(UUID hotel, UUID guest, UUID note) {
        entityManager.createQuery("delete from GuestNoteEntity n where n.hotelId=:hotel and n.guestId=:guest and n.id=:note")
            .setParameter("hotel", hotel).setParameter("guest", guest).setParameter("note", note).executeUpdate();
    }

    public void tag(UUID hotel, UUID guest, String tag) {
        queries.execute("INSERT INTO guest_tag(hotel_id,guest_id,tag) VALUES (:hotel,:guest,:tag) ON CONFLICT DO NOTHING",
            Map.of("hotel", hotel, "guest", guest, "tag", tag));
    }
    public void untag(UUID hotel, UUID guest, String tag) {
        entityManager.createQuery("delete from GuestTagEntity t where t.id.hotelId=:hotel and t.id.guestId=:guest and t.id.tag=:tag")
            .setParameter("hotel", hotel).setParameter("guest", guest).setParameter("tag", tag).executeUpdate();
    }
}
