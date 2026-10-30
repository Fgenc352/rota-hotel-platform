package com.hotelbooking.guest.api;

import com.hotelbooking.guest.api.dto.CreateSegmentRequest;
import com.hotelbooking.guest.api.mapper.CreateSegmentRequestMapper;
import com.hotelbooking.guest.application.command.CreateSegmentHandler;
import com.hotelbooking.guest.application.command.DeleteSegmentHandler;
import com.hotelbooking.guest.application.model.CreatedId;
import com.hotelbooking.guest.application.model.Segment;
import com.hotelbooking.guest.application.query.ListSegmentsHandler;
import com.hotelbooking.identity.application.model.CurrentStaff;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm")
@Validated
@RequiredArgsConstructor
public class SegmentController {
    private final ListSegmentsHandler listSegments;
    private final CreateSegmentHandler createSegment;
    private final DeleteSegmentHandler deleteSegment;
    private final CreateSegmentRequestMapper createSegmentRequestMapper;

    @GetMapping("/segments")
    List<Segment> segments(@Parameter(hidden = true) CurrentStaff staff) {
        return listSegments.handle(new ListSegmentsHandler.Input(staff.hotelId()));
    }

    @PostMapping("/segments")
    CreatedId segment(@Parameter(hidden = true) CurrentStaff staff, @Valid @RequestBody CreateSegmentRequest segment) {
        UUID hotel = staff.hotelId();
        return createSegment.handle(createSegmentRequestMapper.toCommand(hotel, segment));
    }

    @DeleteMapping("/segments/{id}")
    void deleteSegment(@Parameter(hidden = true) CurrentStaff staff, @PathVariable UUID id) {
        UUID hotel = staff.hotelId();
        deleteSegment.handle(new DeleteSegmentHandler.Input(hotel, id));
    }
}
