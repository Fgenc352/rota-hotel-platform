package com.hotelbooking.unit;

import com.hotelbooking.identity.application.model.CurrentStaff;
import com.hotelbooking.pms.api.PmsOperationsController;
import com.hotelbooking.pms.api.mapper.PmsCommandMapper;
import com.hotelbooking.pms.application.command.AssignRoomHandler;
import com.hotelbooking.pms.application.command.ChangeHousekeepingHandler;
import com.hotelbooking.pms.application.query.GetPmsBoardHandler;
import com.hotelbooking.pms.application.query.GetRoomHistoryHandler;
import com.hotelbooking.pms.application.port.RoomBoardQueries;
import com.hotelbooking.pms.application.model.RoomAuditPage;
import java.util.List;
import com.hotelbooking.pms.application.query.GetReservationServicesHandler;
import com.hotelbooking.pms.domain.Housekeeping;
import com.hotelbooking.shared.web.ApiErrors;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("unit")
class PmsApiHttpTest {
    private final UUID hotel = UUID.randomUUID();
    private final UUID reservation = UUID.randomUUID();
    private final UUID room = UUID.randomUUID();
    private final AssignRoomHandler assignments = mock(AssignRoomHandler.class);
    private final ChangeHousekeepingHandler housekeeping = mock(ChangeHousekeepingHandler.class);
    private final RoomBoardQueries historyQueries = mock(RoomBoardQueries.class);
    private final MockMvc http = MockMvcBuilders.standaloneSetup(new PmsOperationsController(
        mock(GetPmsBoardHandler.class), new GetRoomHistoryHandler(historyQueries), assignments, housekeeping, new PmsCommandMapper(),
        mock(GetReservationServicesHandler.class)))
        .setControllerAdvice(new ApiErrors())
        .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(CurrentStaff.class);
        }
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
            NativeWebRequest request, WebDataBinderFactory binder) {
            return new CurrentStaff("authenticated-staff", hotel);
        }
    }).build();

    @Test
    void commandsUseAuthenticatedHotelAndActorIgnoringClientTenantParameters() throws Exception {
        http.perform(post("/api/crm/pms/reservations/" + reservation + "/room")
            .param("hotelId", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON).content("{\"roomId\":\"" + room + "\"}"))
            .andExpect(status().isNoContent());
        verify(assignments).handle(new AssignRoomHandler.Input(hotel, reservation, room, "authenticated-staff"));
        http.perform(post("/api/crm/pms/rooms/" + room + "/housekeeping")
            .contentType(MediaType.APPLICATION_JSON).content("{\"state\":\"CLEAN\"}"))
            .andExpect(status().isNoContent());
        verify(housekeeping).handle(new ChangeHousekeepingHandler.Input(hotel, room, Housekeeping.CLEAN, "authenticated-staff"));

    }

    @Test
    void historyUsesAuthenticatedHotelAndRequestedPage() throws Exception {
        when(historyQueries.historyPage(hotel, 6, 20)).thenReturn(new RoomAuditPage(List.of(), 125, 6, 20));
        http.perform(get("/api/crm/pms/history").param("page", "6").param("size", "20")
            .param("hotelId", UUID.randomUUID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(125))
            .andExpect(jsonPath("$.page").value(6))
            .andExpect(jsonPath("$.size").value(20));
        verify(historyQueries).historyPage(hotel, 6, 20);
    }

    @Test
    void invalidHistoryPagesAreRejectedBeforeQuerying() throws Exception {
        for (String page : new String[] {
            "-1", "1000001", "invalid"
        }) {
            http.perform(get("/api/crm/pms/history").param("page", page)).andExpect(status().isBadRequest());
        }
        for (String size : new String[] {
            "0", "101"
        }) {
            http.perform(get("/api/crm/pms/history").param("size", size)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(historyQueries);
    }

    @Test
    void invalidRoomAndWorkflowInputsNeverReachCommands() throws Exception {
        for (String payload : new String[] {
            "{}", "{\"roomId\":\"invalid\"}"
        }) {
            http.perform(post("/api/crm/pms/reservations/" + reservation + "/room")
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isBadRequest());
        }
        for (String payload : new String[] {
            "{}", "{\"state\":\"BROKEN\"}"
        }) {
            http.perform(post("/api/crm/pms/rooms/" + room + "/housekeeping")
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(assignments, housekeeping);
    }
}
