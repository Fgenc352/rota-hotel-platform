package com.hotelbooking.events.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.messaging")
public record MessagingProperties(
    @DefaultValue("direct") @NotNull Transport transport,
    @DefaultValue("hotel.reservations.v1") @NotBlank String topic,
    @DefaultValue("hotel.reservations.v1.DLT") @NotBlank String deadLetterTopic,
    @DefaultValue("hotel-pms-dispatch-v1") @NotBlank String groupId,
    @DefaultValue("3") @Min(1) int partitions,
    @DefaultValue("1") @Min(1) int replicas,
    @DefaultValue("5s") @NotNull Duration publishTimeout
    ) {
    public enum Transport {
        DIRECT, KAFKA
    }

    @AssertTrue(message = "Kafka publish timeout must be positive")
    public boolean isPublishTimeoutPositive() {
        return publishTimeout == null || publishTimeout.isPositive();
    }

    @AssertTrue(message = "Kafka event topic and dead letter topic must differ")
    public boolean isTopicDistinct() {
        return topic == null || !topic.equals(deadLetterTopic);
    }
}
