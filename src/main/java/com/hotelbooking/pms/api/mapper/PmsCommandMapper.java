package com.hotelbooking.pms.api.mapper;

import com.hotelbooking.identity.application.model.CurrentStaff;
import com.hotelbooking.pms.api.dto.AssignRoomRequest;
import com.hotelbooking.pms.api.dto.HousekeepingRequest;
import com.hotelbooking.pms.application.command.AssignRoomHandler;
import com.hotelbooking.pms.application.command.ChangeHousekeepingHandler;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PmsCommandMapper {
    public AssignRoomHandler.Input assignment(CurrentStaff staff, UUID reservation, AssignRoomRequest request) {
        return new AssignRoomHandler.Input(staff.hotelId(), reservation, request.roomId(), staff.username());
    }
    public ChangeHousekeepingHandler.Input housekeeping(CurrentStaff staff, UUID room, HousekeepingRequest request) {
        return new ChangeHousekeepingHandler.Input(staff.hotelId(), room, request.state(), staff.username());
    }
}
