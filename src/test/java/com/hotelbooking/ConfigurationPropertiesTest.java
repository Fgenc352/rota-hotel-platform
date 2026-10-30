package com.hotelbooking;

import com.hotelbooking.events.config.OutboxProperties;
import com.hotelbooking.events.config.MessagingProperties;
import com.hotelbooking.inventory.config.AvailabilityCacheProperties;
import com.hotelbooking.events.config.PmsProperties;
import com.hotelbooking.hotel.config.BookingProperties;
import com.hotelbooking.identity.config.DemoProperties;
import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ConfigurationPropertiesTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
        .withUserConfiguration(PropertiesConfiguration.class)
        .withPropertyValues(
        "app.booking.eur-rate=40.00",
        "app.demo.seed-password=DemoHotel2026!",
        "app.pms.url=http://localhost:8090",
        "app.pms.connect-timeout=2s",
        "app.pms.request-timeout=3s",
        "app.outbox.batch-size=20",
        "app.outbox.poll-interval=500ms");

    @Test
    void bindsMoneyUrlsAndDurationSettings() {
        context.run(application -> {
            assertThat(application).hasNotFailed();
            assertThat(application.getBean(BookingProperties.class).eurRate()).isEqualByComparingTo("40.00");
            assertThat(application.getBean(PmsProperties.class).connectTimeout()).isEqualTo(Duration.ofSeconds(2));
            assertThat(application.getBean(OutboxProperties.class).pollInterval()).isEqualTo(Duration.ofMillis(500));
        });
    }

    @Test
    void defaultsKeepDirectPmsAndMemoryCacheAndOptionalProvidersCanBeSelected() {
        context.run(application -> {
            assertThat(application).hasNotFailed();
            assertThat(application.getBean(MessagingProperties.class).transport()).isEqualTo(MessagingProperties.Transport.DIRECT);
            assertThat(application.getBean(AvailabilityCacheProperties.class).provider()).isEqualTo(AvailabilityCacheProperties.Provider.MEMORY);
        });
        context.withPropertyValues("app.messaging.transport=kafka", "app.availability-cache.provider=redis").run(application -> {
            assertThat(application).hasNotFailed();
            assertThat(application.getBean(MessagingProperties.class).transport()).isEqualTo(MessagingProperties.Transport.KAFKA);
            assertThat(application.getBean(AvailabilityCacheProperties.class).provider()).isEqualTo(AvailabilityCacheProperties.Provider.REDIS);
        });
    }

    @Test
    void loadsKafkaRedisDefaultsWithoutAnActiveProfile() {
        context.withInitializer(new ConfigDataApplicationContextInitializer())
            .run(application -> {
            assertThat(application).hasNotFailed();
            assertThat(application.getEnvironment().getActiveProfiles()).isEmpty();
            assertThat(application.getBean(MessagingProperties.class).transport()).isEqualTo(MessagingProperties.Transport.KAFKA);
            assertThat(application.getBean(AvailabilityCacheProperties.class).provider()).isEqualTo(AvailabilityCacheProperties.Provider.REDIS);
            assertThat(application.getEnvironment().getProperty("spring.kafka.bootstrap-servers")).isEqualTo("localhost:9092");
            assertThat(application.getEnvironment().getProperty("spring.data.redis.port")).isEqualTo("6379");
        });
    }

    @Test
    void rejectsInvalidCacheAndMessagingSettings() {
        context.withPropertyValues("app.availability-cache.ttl=0s").run(application -> assertThat(application).hasFailed());
        context.withPropertyValues("app.messaging.transport=unsupported").run(application -> assertThat(application).hasFailed());
        context.withPropertyValues("app.messaging.publish-timeout=0s").run(application -> assertThat(application).hasFailed());
    }

    @Test
    void rejectsNonPositiveExchangeRate() {
        context.withPropertyValues("app.booking.eur-rate=0")
            .run(application -> assertThat(application).hasFailed());
    }

    @Test
    void rejectsInvalidPmsUrlAndTimeouts() {
        context.withPropertyValues("app.pms.url=file:///tmp/pms")
            .run(application -> assertThat(application).hasFailed());
        context.withPropertyValues("app.pms.request-timeout=0s")
            .run(application -> assertThat(application).hasFailed());
    }

    @Test
    void rejectsEmptyBatchesAndShortDemoPasswords() {
        context.withPropertyValues("app.outbox.batch-size=0")
            .run(application -> assertThat(application).hasFailed());
        context.withPropertyValues("app.demo.seed-password=short")
            .run(application -> assertThat(application).hasFailed());
    }

    @Test
    void containerAddressOverridesKeepSharedPersistentDataWithoutProfiles() {
        context.withInitializer(new ConfigDataApplicationContextInitializer())
            .withPropertyValues(
                "server.port=8080",
                "server.servlet.session.cookie.name=ROTA_DOCKER_SESSION",
                "spring.datasource.url=jdbc:postgresql://db:5432/hotel",
                "spring.kafka.bootstrap-servers=kafka:29092",
                "spring.data.redis.host=redis",
                "app.pms.url=http://pms:8090")
            .run(application -> {
                assertThat(application).hasNotFailed();
                var environment = application.getEnvironment();
                assertThat(environment.getActiveProfiles()).isEmpty();
                assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://db:5432/hotel");
                assertThat(environment.getProperty("spring.kafka.bootstrap-servers")).isEqualTo("kafka:29092");
                assertThat(environment.getProperty("spring.data.redis.host")).isEqualTo("redis");
                assertThat(environment.getProperty("spring.liquibase.drop-first")).isEqualTo("false");
                assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
                assertThat(environment.getProperty("server.port")).isEqualTo("8080");
                assertThat(environment.getProperty("server.servlet.session.cookie.name")).isEqualTo("ROTA_DOCKER_SESSION");
                assertThat(application.getBean(MessagingProperties.class).topic()).isEqualTo("hotel.reservations.v1");
                assertThat(application.getBean(MessagingProperties.class).groupId()).isEqualTo("hotel-pms-dispatch-v1");
                assertThat(application.getBean(MessagingProperties.class).transport()).isEqualTo(MessagingProperties.Transport.KAFKA);
                assertThat(application.getBean(AvailabilityCacheProperties.class).provider()).isEqualTo(AvailabilityCacheProperties.Provider.REDIS);
                assertThat(application.getBean(AvailabilityCacheProperties.class).namespace()).isEqualTo("hotel:availability:v1");
                assertThat(application.getBean(PmsProperties.class).url().toString()).isEqualTo("http://pms:8090");
            });
        new ApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer())
            .run(application -> {
                var environment = application.getEnvironment();
                assertThat(environment.getActiveProfiles()).isEmpty();
                assertThat(environment.getProperty("spring.liquibase.drop-first")).isEqualTo("false");
                assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
                assertThat(environment.getProperty("server.port")).isEqualTo("8085");
                assertThat(environment.getProperty("app.pms.url")).isEqualTo("http://localhost:8090");
                assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://localhost:5432/hotel");
                assertThat(environment.getProperty("app.messaging.topic")).isEqualTo("hotel.reservations.v1");
                assertThat(environment.getProperty("app.availability-cache.namespace")).isEqualTo("hotel:availability:v1");
            });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
        BookingProperties.class, PmsProperties.class, MessagingProperties.class, AvailabilityCacheProperties.class,
        OutboxProperties.class, DemoProperties.class
    })
    static class PropertiesConfiguration {
    }
}
