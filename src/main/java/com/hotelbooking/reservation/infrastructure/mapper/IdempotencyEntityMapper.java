package com.hotelbooking.reservation.infrastructure.mapper;

import com.hotelbooking.reservation.application.model.IdempotencyEntry;
import com.hotelbooking.reservation.infrastructure.entity.IdempotencyEntity;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyEntityMapper {
    public IdempotencyEntry toModel(IdempotencyEntity entity) {
        return new IdempotencyEntry(entity.getRequestHash(), entity.getResponse());
    }
}
