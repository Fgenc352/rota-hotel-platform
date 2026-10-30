package com.hotelbooking.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.events.application.port.PmsEventInbox;
import com.hotelbooking.events.config.KafkaDeliveryConfig;
import com.hotelbooking.events.config.MessagingProperties;
import com.hotelbooking.events.infrastructure.kafka.KafkaPmsInboxListener;
import com.hotelbooking.events.infrastructure.kafka.KafkaReservationPublisher;
import com.hotelbooking.events.infrastructure.kafka.KafkaTopicProvisioner;
import com.hotelbooking.inventory.application.port.AvailabilityResults;
import com.hotelbooking.inventory.config.AvailabilityCacheProperties;
import com.hotelbooking.inventory.infrastructure.AvailabilityCache;
import com.hotelbooking.inventory.infrastructure.RedisAvailabilityCache;
import com.hotelbooking.shared.json.Json;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Tag("unit")
class InfrastructureAdaptersTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
        .withUserConfiguration(Properties.class, AvailabilityCache.class, RedisAvailabilityCache.class,
        KafkaReservationPublisher.class, KafkaPmsInboxListener.class, KafkaDeliveryConfig.class, KafkaTopicProvisioner.class)
        .withBean(Json.class, () -> new Json(new ObjectMapper().findAndRegisterModules()))
        .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
        .withBean(org.springframework.kafka.core.KafkaAdmin.class, () -> mock(org.springframework.kafka.core.KafkaAdmin.class))
        .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
        .withBean(ConsumerFactory.class, () -> mock(ConsumerFactory.class))
        .withBean(PmsEventInbox.class, () -> mock(PmsEventInbox.class));

    @Test
    void defaultModeUsesOnlyMemoryCacheAndNoKafkaWorkers() {
        context.run(application -> {
            assertThat(application).hasNotFailed().hasSingleBean(AvailabilityResults.class);
            assertThat(application.getBean(AvailabilityResults.class)).isInstanceOf(AvailabilityCache.class);
            assertThat(application).doesNotHaveBean(KafkaReservationPublisher.class).doesNotHaveBean(KafkaPmsInboxListener.class);
            assertThat(application).doesNotHaveBean("pmsKafkaListenerFactory");
        });
    }

    @Test
    void optionalModeSelectsRedisAndCreatesTheKafkaPipelineWithoutChangingPorts() {
        context.withPropertyValues("app.messaging.transport=kafka", "app.availability-cache.provider=redis")
            .run(application -> {
            assertThat(application).hasNotFailed().hasSingleBean(AvailabilityResults.class);
            assertThat(application.getBean(AvailabilityResults.class)).isInstanceOf(RedisAvailabilityCache.class);
            assertThat(application).hasSingleBean(KafkaReservationPublisher.class).hasSingleBean(KafkaPmsInboxListener.class);
            assertThat(application).hasBean("pmsKafkaListenerFactory").hasBean("reservationTopics");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
        MessagingProperties.class, AvailabilityCacheProperties.class
    })
    static class Properties {
    }
}
