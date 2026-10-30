package com.hotelbooking.pms.api;

import com.hotelbooking.identity.application.model.CurrentStaff;
import com.hotelbooking.pms.api.dto.AssignRoomRequest;
import com.hotelbooking.pms.api.dto.HousekeepingRequest;
import com.hotelbooking.pms.application.model.ReservationServices;
import com.hotelbooking.pms.application.query.GetReservationServicesHandler;
import com.hotelbooking.pms.api.mapper.PmsCommandMapper;
import com.hotelbooking.pms.application.command.AssignRoomHandler;
import com.hotelbooking.pms.application.command.ChangeHousekeepingHandler;
import com.hotelbooking.pms.application.model.PmsBoard;
import com.hotelbooking.pms.application.model.RoomAuditPage;
import com.hotelbooking.pms.application.query.GetRoomHistoryHandler;
import com.hotelbooking.pms.application.query.GetPmsBoardHandler;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crm/pms")
@RequiredArgsConstructor
public class PmsOperationsController {
    private final GetPmsBoardHandler board;
    private final GetRoomHistoryHandler history;
    private final AssignRoomHandler assignments;
    private final ChangeHousekeepingHandler housekeeping;
    private final PmsCommandMapper mapper;
    private final GetReservationServicesHandler services;

    @GetMapping("/board")
    PmsBoard board(@Parameter(hidden = true) CurrentStaff staff) {
        return board.handle(staff.hotelId());
    }

    @GetMapping("/history")
    RoomAuditPage history(@Parameter(hidden = true) CurrentStaff staff,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return history.handle(staff.hotelId(), page, size);
    }

    @PostMapping("/reservations/{id}/room")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void assign(@Parameter(hidden = true) CurrentStaff staff, @PathVariable UUID id, @Valid @RequestBody AssignRoomRequest request) {
        assignments.handle(mapper.assignment(staff, id, request));
    }

    @PostMapping("/rooms/{id}/housekeeping")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void clean(@Parameter(hidden = true) CurrentStaff staff, @PathVariable UUID id, @Valid @RequestBody HousekeepingRequest request) {
        housekeeping.handle(mapper.housekeeping(staff, id, request));
    }
    @GetMapping("/reservations/{id}/services")
    ReservationServices services(@Parameter(hidden = true) CurrentStaff staff, @PathVariable UUID id) {
        return services.handle(staff.hotelId(), id);
    }
}
