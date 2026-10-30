package com.hotelbooking.hotel.infrastructure;

import com.hotelbooking.hotel.application.port.HotelCatalog;
import com.hotelbooking.hotel.domain.Hotel;
import com.hotelbooking.hotel.infrastructure.entity.HotelOriginEntity;
import com.hotelbooking.hotel.infrastructure.jpa.HotelJpaRepository;
import com.hotelbooking.hotel.infrastructure.mapper.HotelEntityMapper;
import com.hotelbooking.shared.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class HotelRepository implements HotelCatalog {
    private final HotelJpaRepository hotels;
    private final HotelEntityMapper mapper;
    private final EntityManager entityManager;

    public Hotel byKey(String key) {
        return mapper.toDomain(hotels.findByPublicKey(key).orElseThrow(BusinessException::missing));
    }
    public Hotel byId(UUID id) {
        return mapper.toDomain(hotels.findById(id).orElseThrow(BusinessException::missing));
    }
    public void lock(UUID id) {
        hotels.lockById(id).orElseThrow(BusinessException::missing);
    }
    public String timezone(UUID hotel) {
        return byId(hotel).timezone();
    }
    public boolean allowsOrigin(String key, String origin) {
        return entityManager.createQuery("select count(o) from HotelOriginEntity o join o.hotel h where h.publicKey=:key and o.id.origin=:origin",
            Long.class)
            .setParameter("key", key).setParameter("origin", origin).getSingleResult() > 0;
    }
}
