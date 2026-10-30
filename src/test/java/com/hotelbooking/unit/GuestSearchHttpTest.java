package com.hotelbooking.unit;

import com.hotelbooking.guest.api.GuestController;
import com.hotelbooking.guest.application.port.GuestQueries;
import com.hotelbooking.guest.application.model.GuestPage;
import com.hotelbooking.guest.application.model.GuestSearch;
import com.hotelbooking.guest.application.query.SearchGuestsHandler;
import com.hotelbooking.identity.application.model.CurrentStaff;
import com.hotelbooking.shared.web.ApiErrors;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
class GuestSearchHttpTest {
    @Test
    void mapsIndependentIsoDateFiltersAndUsesAuthenticatedHotel() throws Exception {
        UUID hotel = UUID.randomUUID();
        var queries = mock(GuestQueries.class);
        when(queries.guests(any(), anyString(), anyInt(), anyInt(), anyString(), any(), any(), any()))
            .thenReturn(new GuestPage(List.of(), 0, 0, 20));
        var controller = new GuestController(new SearchGuestsHandler(queries), null, null, null, null, null, null,
            null);
        var http = http(controller, hotel);
        http.perform(get("/api/crm/guests").param("firstName", "Deniz").param("lastName", "Test")
            .param("email", "deniz").param("phone", "555").param("checkInFrom", "2027-01-01")
            .param("checkOutTo", "2027-02-01")
            .param("hotelId", UUID.randomUUID().toString())).andExpect(status().isOk());
        verify(queries).guests(hotel, "", 0, 20, "ALL", new GuestSearch("Deniz", "Test", "deniz", "555",
            LocalDate.of(2027, 1, 1), null, null, LocalDate.of(2027, 2, 1)), com.hotelbooking.guest.application.model.GuestSort.LAST_NAME,
            com.hotelbooking.shared.application.SortDirection.ASC);
        http.perform(get("/api/crm/guests").param("sort", "email; DROP TABLE guest")).andExpect(status().isBadRequest());
        http.perform(get("/api/crm/guests").param("direction", "sideways")).andExpect(status().isBadRequest());
        http.perform(get("/api/crm/guests").param("checkInFrom", "not-a-date")).andExpect(status().isBadRequest());
        http.perform(get("/api/crm/guests").param("checkInFrom", "2027-03-01").param("checkInTo", "2027-01-01"))
            .andExpect(status().isBadRequest());
    }
    @Test
    void reservationSortAndDirectionAreTypedAndUnknownValuesAreRejected() throws Exception {
        UUID hotel = UUID.randomUUID();
        var queries = mock(com.hotelbooking.reservation.application.port.BookingQueries.class);
        when(queries.bookings(any(), anyInt(), anyInt(), any(), any(), any()))
            .thenReturn(new com.hotelbooking.reservation.application.model.BookingPage(List.of(), 0));
        var controller = new com.hotelbooking.reservation.api.StaffReservationController(
            new com.hotelbooking.reservation.application.query.ListBookingsHandler(queries), null, null, null);
        var http = http(controller, hotel);
        http.perform(get("/api/crm/reservations").param("page", "2").param("size", "10")
            .param("sort", "CHECK_IN").param("direction", "ASC")).andExpect(status().isOk());
        verify(queries).bookings(hotel, 2, 10, com.hotelbooking.reservation.application.model.BookingSort.CHECK_IN,
            com.hotelbooking.shared.application.SortDirection.ASC, com.hotelbooking.reservation.application.model.BookingSearch.empty());
        http.perform(get("/api/crm/reservations")).andExpect(status().isOk());
        verify(queries).bookings(hotel, 0, 20, com.hotelbooking.reservation.application.model.BookingSort.CREATED_AT,
            com.hotelbooking.shared.application.SortDirection.DESC, com.hotelbooking.reservation.application.model.BookingSearch.empty());
        http.perform(get("/api/crm/reservations").param("sort", "total desc; DROP TABLE reservation"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void reservationFiltersAreValidatedAndBoundSeparatelyFromTenantIdentity() throws Exception {
        UUID hotel = UUID.randomUUID();
        var queries = mock(com.hotelbooking.reservation.application.port.BookingQueries.class);
        when(queries.bookings(any(), anyInt(), anyInt(), any(), any(), any()))
            .thenReturn(new com.hotelbooking.reservation.application.model.BookingPage(List.of(), 0));
        var controller = new com.hotelbooking.reservation.api.StaffReservationController(
            new com.hotelbooking.reservation.application.query.ListBookingsHandler(queries),
            new com.hotelbooking.reservation.application.query.GetBookingHandler(queries), null, null);
        var http = http(controller, hotel);
        http.perform(get("/api/crm/reservations").param("firstName", "Deniz").param("children", "0")
            .param("status", "CONFIRMED").param("checkInFrom", "2027-02-10").param("hotelId", UUID.randomUUID().toString()))
            .andExpect(status().isOk());
        verify(queries).bookings(eq(hotel), eq(0), eq(20), any(), any(), argThat(search -> search.firstName().equals("Deniz")
            && search.children() == 0 && search.checkInFrom().equals(LocalDate.of(2027, 2, 10))));
        http.perform(get("/api/crm/reservations").param("checkInFrom", "2027-02-12").param("checkInTo", "2027-02-10"))
            .andExpect(status().isBadRequest());
        http.perform(get("/api/crm/reservations").param("minTotal", "500").param("maxTotal", "10"))
            .andExpect(status().isBadRequest());
        http.perform(get("/api/crm/reservations").param("status", "INVALID")).andExpect(status().isBadRequest());
        http.perform(get("/api/crm/reservations").param("children", "-1")).andExpect(status().isBadRequest());
        UUID reservation = UUID.randomUUID();
        http.perform(get("/api/crm/reservations/"+reservation)).andExpect(status().isOk());
        verify(queries).detail(hotel, reservation);
    }

    @Test
    void noteDeletionUsesTheAuthenticatedActorAndHotelRatherThanClientParameters() throws Exception {
        UUID hotel = UUID.randomUUID(), guest = UUID.randomUUID(), note = UUID.randomUUID();
        var handler = mock(com.hotelbooking.guest.application.command.DeleteGuestNoteHandler.class);
        var controller = new GuestController(null, null, null, handler, null, null, null, null);
        http(controller, hotel).perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .delete("/api/crm/guests/" + guest + "/notes/" + note)
            .param("hotelId", UUID.randomUUID().toString()).param("actor", "forged"))
            .andExpect(status().isNoContent());
        verify(handler).handle(new com.hotelbooking.guest.application.command.DeleteGuestNoteHandler.Input(hotel, guest, note, "staff"));
    }

    private org.springframework.test.web.servlet.MockMvc http(Object controller, UUID hotel) {
        return MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ApiErrors())
            .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(CurrentStaff.class);
            }
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                NativeWebRequest request, WebDataBinderFactory binder) {
                return new CurrentStaff("staff", hotel);
            }
        }).build();
    }

}
