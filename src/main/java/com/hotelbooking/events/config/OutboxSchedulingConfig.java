package com.hotelbooking.events.config;

import com.hotelbooking.events.application.EventConsumer;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.worker-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxSchedulingConfig {
    @Bean
    ThreadPoolTaskScheduler taskScheduler(List<EventConsumer> consumers) {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Math.max(1, consumers.size()));
        scheduler.setThreadNamePrefix("outbox-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        return scheduler;
    }
}
