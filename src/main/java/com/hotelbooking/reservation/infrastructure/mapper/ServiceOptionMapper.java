package com.hotelbooking.reservation.infrastructure.mapper;

import com.hotelbooking.reservation.domain.ServiceOption;
import com.hotelbooking.reservation.infrastructure.entity.HotelServiceEntity;
import org.springframework.stereotype.Component;

@Component
public class ServiceOptionMapper {
    public ServiceOption toModel(HotelServiceEntity entity) {
        return new ServiceOption(entity.getCode(), entity.getName(), entity.getDescription(), entity.getPrice(),
            entity.getUnit(), entity.isIncluded());
    }
}
