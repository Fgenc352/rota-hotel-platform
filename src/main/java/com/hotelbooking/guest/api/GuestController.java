package com.hotelbooking.guest.api;

import com.hotelbooking.guest.api.dto.AddTagRequest;
import com.hotelbooking.guest.api.dto.CreateNoteRequest;
import com.hotelbooking.guest.api.mapper.GuestNoteRequestMapper;
import com.hotelbooking.guest.api.mapper.GuestTagRequestMapper;
import com.hotelbooking.guest.application.command.AddGuestNoteHandler;
import com.hotelbooking.guest.application.command.DeleteGuestNoteHandler;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.hotelbooking.guest.application.command.AddGuestTagHandler;
import com.hotelbooking.guest.application.command.RemoveGuestTagHandler;
import com.hotelbooking.guest.application.model.CreatedId;
import com.hotelbooking.guest.application.model.GuestDetail;
import com.hotelbooking.guest.application.model.GuestPage;
import com.hotelbooking.guest.application.query.GetGuestDetailHandler;
import com.hotelbooking.guest.application.query.SearchGuestsHandler;
import com.hotelbooking.identity.application.model.CurrentStaff;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import com.hotelbooking.guest.application.model.GuestSort;
import com.hotelbooking.shared.application.SortDirection;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.hotelbooking.guest.application.model.GuestSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm")
@Validated
@RequiredArgsConstructor
public class GuestController {
    private final SearchGuestsHandler searchGuests;
    private final GetGuestDetailHandler getGuestDetail;
    private final AddGuestNoteHandler addGuestNote;
    private final DeleteGuestNoteHandler deleteGuestNote;
    private final AddGuestTagHandler addGuestTag;
    private final RemoveGuestTagHandler removeGuestTag;
    private final GuestNoteRequestMapper guestNoteRequestMapper;
    private final GuestTagRequestMapper guestTagRequestMapper;

    @GetMapping("/guests")
    GuestPage guests(@Parameter(hidden = true) CurrentStaff staff, @RequestParam(defaultValue = "") @Size(max = 100) String q,
        @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "ALL") @Size(max = 80) String segment,
        @RequestParam(defaultValue = "") @Size(max = 100) String firstName,
        @RequestParam(defaultValue = "") @Size(max = 100) String lastName,
        @RequestParam(defaultValue = "") @Size(max = 254) String email,
        @RequestParam(defaultValue = "") @Size(max = 20) String phone,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInTo,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutTo,
        @RequestParam(defaultValue = "LAST_NAME") GuestSort sort,
        @RequestParam(defaultValue = "ASC") SortDirection direction) {
        UUID hotel = staff.hotelId();
        return searchGuests.handle(new SearchGuestsHandler.Input(hotel, q, page, size, segment,
            new GuestSearch(firstName, lastName, email, phone,
            checkInFrom, checkInTo, checkOutFrom, checkOutTo), sort, direction));
    }

    @GetMapping("/guests/{id}")
    GuestDetail detail(@Parameter(hidden = true) CurrentStaff staff, @PathVariable UUID id) {
        UUID hotel = staff.hotelId();
        return getGuestDetail.handle(new GetGuestDetailHandler.Input(hotel, id));
    }

    @PostMapping("/guests/{id}/notes")
    CreatedId note(@Parameter(hidden = true) CurrentStaff staff, @PathVariable UUID id, @Valid @RequestBody CreateNoteRequest note) {
        UUID hotel = staff.hotelId();
        return addGuestNote.handle(guestNoteRequestMapper.toCommand(hotel, id, staff.username(), note));
    }

    @Operation(summary = "Misafir notunu sil", description = "Yalnızca notu yazan personel ilk 24 saat içinde silebilir. Tam 24 saat dolduğunda silme reddedilir.")
    @ApiResponse(responseCode = "204", description = "Not silindi")
    @ApiResponse(responseCode = "403", description = "Notu yazan personel değilsiniz")
    @ApiResponse(responseCode = "404", description = "Bu otel ve profilde not bulunamadı")
    @ApiResponse(responseCode = "409", description = "24 saatlik silme süresi doldu")
    @DeleteMapping("/guests/{id}/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteNote(@Parameter(hidden = true) CurrentStaff staff, @PathVariable UUID id, @PathVariable UUID noteId) {
        deleteGuestNote.handle(new DeleteGuestNoteHandler.Input(staff.hotelId(), id, noteId, staff.username()));
    }

    @PostMapping("/guests/{id}/tags")
    void tag(@Parameter(hidden = true) CurrentStaff staff, @PathVariable UUID id, @Valid @RequestBody AddTagRequest tag) {
        UUID hotel = staff.hotelId();
        addGuestTag.handle(guestTagRequestMapper.toCommand(hotel, id, tag));
    }

    @DeleteMapping("/guests/{id}/tags/{tag}")
    void untag(@Parameter(hidden = true) CurrentStaff staff, @PathVariable UUID id, @PathVariable String tag) {
        UUID hotel = staff.hotelId();
        removeGuestTag.handle(new RemoveGuestTagHandler.Input(hotel, id, tag));
    }
}
