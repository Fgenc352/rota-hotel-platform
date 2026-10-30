package com.hotelbooking;

import com.hotelbooking.events.config.OutboxProperties;
import com.hotelbooking.events.config.PmsProperties;
import com.hotelbooking.hotel.config.BookingProperties;
import com.hotelbooking.identity.config.DemoProperties;
import com.hotelbooking.identity.config.IdentityWebConfig;
import com.hotelbooking.identity.config.SecurityConfig;
import com.hotelbooking.support.TestTransactionManager;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = SwaggerUiTest.DocumentationApplication.class, properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
    "spring.liquibase.enabled=false", "app.worker-enabled=false"
})
@AutoConfigureMockMvc
@TestExecutionListeners({
    ServletTestExecutionListener.class, DependencyInjectionTestExecutionListener.class
})
@Tag("functional")
class SwaggerUiTest {
    @Autowired
    private MockMvc http;

    @Test
    void swaggerAssetsAndConfigurationAreAccessibleWithoutCredentials() throws Exception {
        http.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk());
        http.perform(get("/v3/api-docs/swagger-config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.url").value("/v3/api-docs"));
    }

    @Test
    void generatedContractDocumentsAuthenticationAndKeepsStaffEndpointsProtected() throws Exception {
        var contract = http.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"))
            .andExpect(jsonPath("$.paths['/api/crm/guests'].get.security[0].basicAuth").isArray())
            .andExpect(jsonPath("$.paths['/api/public/hotels/{key}/availability'].get.security").doesNotExist())
            .andExpect(jsonPath("$.paths['/api/crm/guests/{id}/notes/{noteId}'].delete.security[0].basicAuth").isArray())
            .andExpect(jsonPath("$.paths['/api/crm/guests/{id}/notes/{noteId}'].delete.responses['204']").exists())
            .andExpect(jsonPath("$.paths['/api/crm/guests/{id}/notes/{noteId}'].delete.responses['403']").exists())
            .andExpect(jsonPath("$.paths['/api/crm/guests/{id}/notes/{noteId}'].delete.responses['409']").exists())
            .andExpect(jsonPath("$.paths['/api/crm/guests/{id}/matching']").doesNotExist())
            .andExpect(jsonPath("$.components.schemas.GuestSummary.properties.matching_conflict").doesNotExist())
            .andExpect(jsonPath("$.components.schemas.GuestDetail.properties.emails").exists())
            .andExpect(jsonPath("$.components.schemas.GuestDetail.properties.phones").exists())
            .andExpect(jsonPath("$.components.schemas.CreateSegmentRequest.properties.daysSinceCheckout").doesNotExist())
            .andExpect(jsonPath("$.components.schemas.Segment.properties.days_since_checkout").doesNotExist())
            .andExpect(jsonPath("$.components.schemas.GuestSummary.properties.first_name").exists())
            .andExpect(jsonPath("$.components.schemas.GuestSummary.properties.marketing_allowed").exists())
            .andExpect(jsonPath("$.paths['/api/crm/reservations/{id}'].get.security[0].basicAuth").isArray())
            .andExpect(jsonPath("$.paths['/api/public/hotels/{key}/services'].get").exists())
            .andExpect(jsonPath("$.components.schemas.BookingSummary.properties.guest_id").exists())
            .andExpect(jsonPath("$.components.schemas.BookingSummary.properties.services").exists())
            .andExpect(jsonPath("$.paths['/api/crm/pms/board'].get.security[0].basicAuth").isArray())
            .andExpect(jsonPath("$.paths['/api/crm/pms/history'].get.security[0].basicAuth").isArray())
            .andExpect(jsonPath("$.paths['/api/crm/pms/daily'].get.security[0].basicAuth").isArray())
            .andExpect(jsonPath("$.paths['/api/crm/pms/daily'].get.parameters[0].required").value(false))
            .andExpect(jsonPath("$.paths['/api/crm/pms/reservations/{id}/room'].post").exists())
            .andExpect(jsonPath("$.paths['/api/crm/pms/rooms/{id}/housekeeping'].post").exists())
            .andExpect(jsonPath("$.paths['/api/crm/pms/reservations/{id}/services/{code}']").doesNotExist())
            .andReturn();
        String output = System.getProperty("hotel.openapi.output");
        if (output != null) {
            java.nio.file.Files.writeString(java.nio.file.Path.of(output), contract.getResponse().getContentAsString());
        }
        http.perform(get("/api/crm/guests"))
            .andExpect(status().isUnauthorized());
        http.perform(get("/api/crm/pms/board"))
            .andExpect(status().isUnauthorized());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableConfigurationProperties({
        BookingProperties.class, DemoProperties.class, OutboxProperties.class, PmsProperties.class
    })
    @Import({
        SecurityConfig.class, IdentityWebConfig.class, com.hotelbooking.reservation.event.ReservationEventPublisher.class
    })
    @ComponentScan(basePackages = {
        "com.hotelbooking.analytics.api",
        "com.hotelbooking.analytics.application",
        "com.hotelbooking.events.api",
        "com.hotelbooking.events.application",
        "com.hotelbooking.guest.api",
        "com.hotelbooking.guest.application",
        "com.hotelbooking.hotel.api",
        "com.hotelbooking.hotel.application",
        "com.hotelbooking.identity.api",
        "com.hotelbooking.identity.application",
        "com.hotelbooking.inventory.api",
        "com.hotelbooking.inventory.application",
        "com.hotelbooking.pms.api",
        "com.hotelbooking.pms.application",
        "com.hotelbooking.reservation.api",
        "com.hotelbooking.reservation.application",
        "com.hotelbooking.shared.web",
        "com.hotelbooking.shared.config",
        "com.hotelbooking.shared.json"
    })
    static class DocumentationApplication {
        @Bean
        UserDetailsService documentationUsers() {
            return username -> {
                throw new UsernameNotFoundException("No documentation test users");
            };
        }

        @Bean
        TransactionTemplate transactionTemplate() {
            return new TransactionTemplate(new TestTransactionManager());
        }

        @Bean
        static BeanFactoryPostProcessor unusedPersistencePorts() {
            return factory -> {
                Class<?>[] ports = {
                    com.hotelbooking.contracts.pms.RoomStayLifecycle.class,
                    com.hotelbooking.pms.application.port.RoomOperations.class,
                    com.hotelbooking.pms.application.port.ServiceTasks.class,
                    com.hotelbooking.pms.application.port.DailyServiceTasks.class,
                    com.hotelbooking.pms.application.port.RoomBoardQueries.class,
                    com.hotelbooking.events.application.port.FailedEventQueries.class,
                    com.hotelbooking.events.application.port.EventRetries.class,
                    com.hotelbooking.events.application.port.EventDeliveries.class,
                    com.hotelbooking.events.application.port.EventOutbox.class,
                    com.hotelbooking.guest.application.port.GuestQueries.class,
                    com.hotelbooking.guest.application.port.GuestCommands.class,
                    com.hotelbooking.guest.application.port.SegmentQueries.class,
                    com.hotelbooking.guest.application.port.SegmentCommands.class,
                    com.hotelbooking.guest.application.port.GuestProjection.class,
                    com.hotelbooking.hotel.application.port.HotelCatalog.class,
                    com.hotelbooking.identity.application.port.StaffAccounts.class,
                    com.hotelbooking.inventory.application.port.InventoryCommands.class,
                    com.hotelbooking.inventory.application.port.RoomCatalog.class,
                    com.hotelbooking.inventory.application.port.AvailabilityQueries.class,
                    com.hotelbooking.inventory.application.port.AvailabilityResults.class,
                    com.hotelbooking.reservation.application.port.BookingQueries.class,
                    com.hotelbooking.reservation.application.port.ServiceCatalog.class,
                    com.hotelbooking.reservation.application.port.ReservationExtras.class,
                    com.hotelbooking.reservation.application.port.IdempotencyRecords.class,
                    com.hotelbooking.reservation.application.port.FunnelEvents.class,
                    com.hotelbooking.reservation.application.port.Reservations.class,
                    com.hotelbooking.reservation.application.port.ReservationSnapshots.class,
                    com.hotelbooking.analytics.application.port.AnalyticsQueries.class
                };
                for (Class<?> port : ports) {
                    Object stub = Proxy.newProxyInstance(port.getClassLoader(), new Class<?>[] {
                        port
                    }, (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "toString" -> "Unused documentation port " + port.getSimpleName();
                                case "hashCode" -> System.identityHashCode(proxy);
                                case "equals" -> proxy == args[0];
                                default -> null;
                            };
                        }
                        throw new AssertionError("Documentation tests must not query persistence: " + port.getSimpleName());
                    });
                    factory.registerSingleton("documentation" + port.getSimpleName(), stub);
                }
            };
        }
    }
}
