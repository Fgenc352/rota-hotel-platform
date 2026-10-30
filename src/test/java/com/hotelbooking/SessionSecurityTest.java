package com.hotelbooking;

import com.hotelbooking.identity.api.CurrentStaffResolver;
import com.hotelbooking.identity.api.IdentityController;
import com.hotelbooking.identity.application.model.CurrentStaff;
import com.hotelbooking.identity.application.model.StaffCredentials;
import com.hotelbooking.identity.application.model.StaffIdentity;
import com.hotelbooking.identity.application.query.GetStaffHotelHandler;
import com.hotelbooking.identity.application.query.GetStaffIdentityHandler;
import com.hotelbooking.identity.config.DemoProperties;
import com.hotelbooking.identity.config.IdentityWebConfig;
import com.hotelbooking.identity.config.SecurityConfig;
import com.hotelbooking.identity.infrastructure.DemoStaffInitializer;
import com.hotelbooking.identity.infrastructure.StaffRepository;
import com.hotelbooking.identity.infrastructure.StaffUserDetailsService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("functional")
@SpringBootTest(classes = SessionSecurityTest.TestApplication.class, properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "spring.liquibase.enabled=false",
    "server.servlet.session.cookie.name=ROTA_DOCKER_SESSION"
})
@AutoConfigureMockMvc
@TestExecutionListeners({
    ServletTestExecutionListener.class, DependencyInjectionTestExecutionListener.class
})
class SessionSecurityTest {
    @Autowired
    MockMvc http;

    @Test
    void loginPersistsAuthenticationForRequestsWithoutBasicCredentials() throws Exception {
        var login = http.perform(get("/api/auth/me").header("Authorization", basic("DemoHotel2026!")))
            .andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        http.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("lagos"));
    }

    @Test
    void cookieAuthenticatedWritesRequireCsrfAndLogoutInvalidatesTheSession() throws Exception {
        var login = http.perform(get("/api/auth/me").header("Authorization", basic("DemoHotel2026!")))
            .andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        String csrf = login.getResponse().getHeader("X-CSRF-TOKEN");
        assertThat(csrf).isNotBlank();
        http.perform(post("/api/crm/probe").session(session)).andExpect(status().isForbidden());
        http.perform(post("/api/crm/probe").session(session).header("X-CSRF-TOKEN", csrf))
            .andExpect(status().isOk());
        http.perform(post("/api/auth/logout").session(session).header("X-CSRF-TOKEN", csrf))
            .andExpect(status().isNoContent())
            .andExpect(cookie().maxAge("ROTA_DOCKER_SESSION", 0));
        assertThat(session.isInvalid()).isTrue();
        http.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void explicitBasicClientsRemainUsableAndInvalidCredentialsAreRejected() throws Exception {
        http.perform(post("/api/crm/probe").header("Authorization", basic("DemoHotel2026!")))
            .andExpect(status().isOk());
        http.perform(get("/api/auth/me").header("Authorization", basic("wrong-pass")))
            .andExpect(status().isUnauthorized());
        http.perform(get("/api/crm/probe")).andExpect(status().isUnauthorized());
    }

    @Test
    void startupSynchronizesOldDemoPasswordsAndBothUsersReceiveTheirOwnHotel() throws Exception {
        http.perform(get("/api/auth/me").header("Authorization", basic("previous-demo-password")))
            .andExpect(status().isUnauthorized());
        http.perform(get("/api/auth/me").header("Authorization", basic("DemoHotel2026!")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.hotelName").value("Lagos Beach Hotel"));
        String peri = "Basic " + Base64.getEncoder().encodeToString("peri:DemoHotel2026!".getBytes(StandardCharsets.UTF_8));
        http.perform(get("/api/auth/me").header("Authorization", peri))
            .andExpect(status().isOk()).andExpect(jsonPath("$.hotelName").value("Peri Stone Cave Hotel"));
    }

    @Test
    void staffContextIgnoresTenantAndUsernameProvidedByTheClient() throws Exception {
        http.perform(get("/api/crm/current-staff")
            .param("hotelId", "10000000-0000-0000-0000-000000000002").param("username", "peri")
            .header("Authorization", basic("DemoHotel2026!")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hotelId").value("10000000-0000-0000-0000-000000000001"));
    }

    private String basic(String password) {
        return "Basic " + Base64.getEncoder().encodeToString(("lagos:" + password).getBytes(StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST"})
    void removedMatchingEndpointReturns404InsteadOfAnInternalError(String method) throws Exception {
        String path = "/api/crm/guests/30000000-0000-0000-0000-000000000001/matching";
        http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .request(org.springframework.http.HttpMethod.valueOf(method), path)
            .header("Authorization", basic("DemoHotel2026!")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.detail").value("İstenen adres bulunamadı."));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableConfigurationProperties(DemoProperties.class)
    @Import({
        SecurityConfig.class, ProbeController.class, IdentityController.class,
        com.hotelbooking.shared.web.ApiErrors.class,
        GetStaffIdentityHandler.class, GetStaffHotelHandler.class, CurrentStaffResolver.class, IdentityWebConfig.class,
        StaffUserDetailsService.class, DemoStaffInitializer.class
    })
    static class TestApplication {
        @Bean
        StaffRepository testStaffRepository(PasswordEncoder encoder) {
            // Real initializer, BCrypt, UserDetailsService and identity controller run against this fixture.
            var passwords = new HashMap<String, String>();
            passwords.put("lagos", encoder.encode("previous-demo-password"));
            passwords.put("peri", encoder.encode("previous-demo-password"));
            return new StaffRepository(null, null, null, null) {
                @Override
                public Optional<StaffCredentials> credentials(String username) {
                    return Optional.ofNullable(passwords.get(username))
                        .map(hash -> new StaffCredentials(username, hash));
                }

                @Override
                public void saveDemoAccount(String username, String hash, UUID hotel) {
                    passwords.put(username, hash);
                }

                @Override
                public UUID hotel(String username) {
                    return identity(username).hotelId();
                }

                @Override
                public StaffIdentity identity(String username) {
                    boolean peri = username.equals("peri");
                    return new StaffIdentity(username,
                        UUID.fromString("10000000-0000-0000-0000-00000000000" + (peri ? "2" : "1")),
                        peri ? "peri-stone" : "lagos-beach",
                        peri ? "Peri Stone Cave Hotel" : "Lagos Beach Hotel", peri ? "Kapadokya" : "Antalya");
                }
            };
        }
    }

    @RestController
    static class ProbeController {
        @GetMapping("/api/crm/current-staff")
        Map<String, String> current(CurrentStaff staff) {
            return Map.of("hotelId", staff.hotelId().toString());
        }

        @GetMapping("/api/crm/probe")
        Map<String, Boolean> read() {
            return Map.of("ok", true);
        }

        @PostMapping("/api/crm/probe")
        Map<String, Boolean> write() {
            return Map.of("ok", true);
        }
    }
}
