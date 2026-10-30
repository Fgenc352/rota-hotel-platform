package com.hotelbooking.reservation.infrastructure;

import com.hotelbooking.reservation.application.port.ServiceCatalog;
import com.hotelbooking.reservation.domain.ServiceOption;
import com.hotelbooking.reservation.infrastructure.entity.HotelServiceEntity;
import com.hotelbooking.reservation.infrastructure.mapper.ServiceOptionMapper;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceCatalogRepository implements ServiceCatalog {
    private final EntityManager entities;
    private final ServiceOptionMapper mapper;
    public List<ServiceOption> services(UUID hotel) {
        return entities.createQuery("select s from HotelServiceEntity s where s.hotelId=:hotel and s.active=true order by s.included desc,s.code",
            HotelServiceEntity.class) .setParameter("hotel",
            hotel).getResultList().stream().map(mapper::toModel).toList();
    }
}
