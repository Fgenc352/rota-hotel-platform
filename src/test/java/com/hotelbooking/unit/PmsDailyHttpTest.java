package com.hotelbooking.unit;

import com.hotelbooking.identity.application.model.CurrentStaff;
import com.hotelbooking.pms.api.PmsDailyController;
import com.hotelbooking.pms.api.mapper.DailyServiceCommandMapper;
import com.hotelbooking.pms.application.command.UpdateDailyServiceHandler;
import com.hotelbooking.pms.application.query.GetDailyOperationsHandler;
import com.hotelbooking.pms.application.query.GetDailyServiceHistoryHandler;
import com.hotelbooking.shared.web.ApiErrors;
import java.time.LocalDate;
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
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
class PmsDailyHttpTest {
    private final UUID hotel = UUID.randomUUID(), reservation = UUID.randomUUID();
    private final GetDailyOperationsHandler daily = mock(GetDailyOperationsHandler.class);
    private final UpdateDailyServiceHandler updates = mock(UpdateDailyServiceHandler.class);
    private final GetDailyServiceHistoryHandler history = mock(GetDailyServiceHistoryHandler.class);
    private final MockMvc http = MockMvcBuilders.standaloneSetup(new PmsDailyController(daily, updates, history,
        new DailyServiceCommandMapper()))
        .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
        new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()
        .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)))
        .setControllerAdvice(new ApiErrors()).setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(CurrentStaff.class);
        }
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
            NativeWebRequest request, WebDataBinderFactory binder) {
            return new CurrentStaff("staff", hotel);
        }
    }).build();

    @Test
    void dailyPlanAndHistoryUseAuthenticatedHotel() throws Exception {
        http.perform(get("/api/crm/pms/daily").param("date", "2026-10-28").param("hotelId", UUID.randomUUID().toString()))
            .andExpect(status().isOk());
        verify(daily).handle(hotel, LocalDate.of(2026, 10, 28));
        http.perform(get("/api/crm/pms/daily")).andExpect(status().isOk());
        verify(daily).handle(hotel, null);
        http.perform(get("/api/crm/pms/reservations/"+reservation+"/daily-services/TRANSFER/history").param("date",
            "2026-10-28"))
            .andExpect(status().isOk());
        verify(history).handle(hotel, reservation, "TRANSFER", LocalDate.of(2026, 10, 28));
    }

    @Test
    void dailyCommandsUseStaffActorAndRejectMissingDateInvalidStateAndOversizedFields() throws Exception {
        String path = "/api/crm/pms/reservations/"+reservation+"/daily-services/TRANSFER";
        http.perform(post(path).contentType(MediaType.APPLICATION_JSON)
            .content("{\"date\":\"2026-10-28\",\"status\":\"PLANNED\",\"time\":\"09:30\",\"pickup\":\"Airport\",\"destination\":\"Hotel\",\"passengers\":2}"))
            .andExpect(status().isNoContent());
        verify(updates).handle(argThat(input -> input.hotel().equals(hotel) && input.actor().equals("staff") && input.passengers() == 2));
        clearInvocations(updates);
        for (String json : new String[] {
            "{}", "{\"date\":\"2026-10-28\",\"status\":\"PLANNED\",\"actor\":\"forged\"}", "{\"date\":\"invalid\",\"status\":\"PLANNED\"}",
            "{\"date\":\"2026-10-28\",\"status\":\"INVALID\"}",
            "{\"date\":\"2026-10-28\",\"status\":\"PLANNED\",\"passengers\":-1}",
            "{\"date\":\"2026-10-28\",\"status\":\"PLANNED\",\"notes\":\""+"x".repeat(1001)+"\"}"
        }) {
            http.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(updates);
        http.perform(get("/api/crm/pms/daily").param("date", "yesterday")).andExpect(status().isBadRequest());
    }
}
