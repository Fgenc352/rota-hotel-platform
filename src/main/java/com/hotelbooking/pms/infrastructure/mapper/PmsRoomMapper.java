package com.hotelbooking.pms.infrastructure.mapper;

import com.hotelbooking.pms.domain.PhysicalRoom;
import com.hotelbooking.pms.domain.RoomAssignment;
import com.hotelbooking.pms.infrastructure.entity.PmsAssignmentEntity;
import com.hotelbooking.pms.infrastructure.entity.PmsRoomEntity;
import org.springframework.stereotype.Component;

@Component
public class PmsRoomMapper {
    public PhysicalRoom room(PmsRoomEntity entity) {
        return new PhysicalRoom(entity.getId(), entity.getRoomTypeId(), entity.getNumber(), entity.getHousekeeping());
    }
    public RoomAssignment assignment(PmsAssignmentEntity entity) {
        return new RoomAssignment(entity.getReservationId(), entity.getRoomId(), entity.getCode(), entity.getCheckIn(),
            entity.getCheckOut(), entity.getStatus());
    }
}
