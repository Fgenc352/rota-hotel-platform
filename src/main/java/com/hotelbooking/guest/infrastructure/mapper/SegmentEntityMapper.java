package com.hotelbooking.guest.infrastructure.mapper;

import com.hotelbooking.guest.application.model.Segment;
import com.hotelbooking.guest.infrastructure.entity.GuestSegmentEntity;
import org.springframework.stereotype.Component;

@Component
public class SegmentEntityMapper {
    public Segment toModel(GuestSegmentEntity entity) {
        return new Segment(entity.getId(), entity.getHotelId(), entity.getName(), entity.getMinStays(), entity.getMonths(),
            entity.isMarketingRequired());
    }
}
