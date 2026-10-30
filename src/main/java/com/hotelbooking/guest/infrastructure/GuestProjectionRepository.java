package com.hotelbooking.guest.infrastructure;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.guest.application.model.AppliedReservation;
import com.hotelbooking.guest.application.port.GuestProjection;
import com.hotelbooking.guest.infrastructure.entity.*;
import com.hotelbooking.guest.infrastructure.jpa.GuestJpaRepository;
import com.hotelbooking.guest.infrastructure.mapper.CrmReservationEntityMapper;
import com.hotelbooking.guest.infrastructure.mapper.GuestEntityMapper;
import com.hotelbooking.hotel.application.port.HotelCatalog;
import com.hotelbooking.shared.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class GuestProjectionRepository implements GuestProjection {
    private final EntityManager entityManager;
    private final HotelCatalog hotels;
    private final GuestJpaRepository guests;
    private final GuestEntityMapper guestMapper;
    private final CrmReservationEntityMapper reservationMapper;
    private final GuestProfileMerger profileMerger;

    public void lockHotel(UUID hotel) {
        hotels.lock(hotel);
    }
    public Optional<AppliedReservation> applied(UUID hotel, UUID reservation) {
        return Optional.ofNullable(entityManager.find(CrmReservationEntity.class, new CrmReservationId(hotel,
            reservation)))
            .map(reservationMapper::toApplied);
    }
    public java.util.List<UUID> findContactCandidates(UUID hotel, String email, String phone) {
        return entityManager.createQuery("""
            select distinct i.guestId from GuestIdentifierEntity i
            where i.id.hotelId=:hotel and (i.id.email=:email or i.id.phone=:phone)
            and i.guest.mergedInto is null
            order by i.guestId
            """,
            UUID.class).setParameter("hotel", hotel)
            .setParameter("email", email.strip().toLowerCase(java.util.Locale.ROOT))
            .setParameter("phone", phone).getResultList();
    }
    public void mergeGuests(UUID hotel, UUID source, UUID target) {
        profileMerger.merge(hotel, source, target);
    }
    public void insertGuest(UUID guest, ReservationSnapshot snapshot) {
        entityManager.persist(guestMapper.toEntity(guest, snapshot));
    }
    public void registerIdentity(UUID guest, ReservationSnapshot snapshot) {
        register(new GuestIdentifierId(snapshot.hotelId(), snapshot.email(), snapshot.phone()), guest);
    }
    private void register(GuestIdentifierId id, UUID guest) {
        if (entityManager.find(GuestIdentifierEntity.class, id) == null) {
            entityManager.persist(GuestIdentifierEntity.builder().id(id).guestId(guest).build());
        }
    }
    public void updateConsent(UUID guest, ReservationSnapshot snapshot) {
        guests.findByIdAndHotelId(guest, snapshot.hotelId()).orElseThrow(BusinessException::missing)
            .updateConsent(snapshot.marketingAllowed(), snapshot.consentAt());
    }
    public void upsertReservation(UUID guest, ReservationSnapshot snapshot) {
        var existing = entityManager.find(CrmReservationEntity.class, new CrmReservationId(snapshot.hotelId(),
            snapshot.id()));
        if (existing == null) {
            entityManager.persist(reservationMapper.toEntity(guest, snapshot));
        } else {
            existing.apply(snapshot.status(), snapshot.total(), snapshot.version(), snapshot.checkedOutAt());
        }
    }
}
