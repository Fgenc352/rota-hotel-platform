package com.hotelbooking.pms.api;

import com.hotelbooking.identity.application.model.CurrentStaff;
import com.hotelbooking.pms.api.dto.DailyServiceRequest;
import com.hotelbooking.pms.api.mapper.DailyServiceCommandMapper;
import com.hotelbooking.pms.application.command.UpdateDailyServiceHandler;
import com.hotelbooking.pms.application.model.DailyOperations;
import com.hotelbooking.pms.application.model.DailyServiceAudit;
import com.hotelbooking.pms.application.query.GetDailyOperationsHandler;
import com.hotelbooking.pms.application.query.GetDailyServiceHistoryHandler;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crm/pms")
@RequiredArgsConstructor
public class PmsDailyController {
    private final GetDailyOperationsHandler daily;
    private final UpdateDailyServiceHandler updates;
    private final GetDailyServiceHistoryHandler history;
    private final DailyServiceCommandMapper mapper;

    @GetMapping("/daily")
    DailyOperations daily(@Parameter(hidden = true) CurrentStaff staff,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return daily.handle(staff.hotelId(), date);
    }

    @PostMapping("/reservations/{id}/daily-services/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@Parameter(hidden = true) CurrentStaff staff, @PathVariable UUID id, @PathVariable String code,
        @Valid @RequestBody DailyServiceRequest request) {
        updates.handle(mapper.command(staff, id, code, request));
    }

    @GetMapping("/reservations/{id}/daily-services/{code}/history")
    List<DailyServiceAudit> history(@Parameter(hidden = true) CurrentStaff staff, @PathVariable UUID id, @PathVariable String code,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return history.handle(staff.hotelId(), id, code, date);
    }
}
