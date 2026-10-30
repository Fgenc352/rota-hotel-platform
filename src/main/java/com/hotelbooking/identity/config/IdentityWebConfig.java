package com.hotelbooking.identity.config;

import com.hotelbooking.identity.api.CurrentStaffResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class IdentityWebConfig implements WebMvcConfigurer {
    private final CurrentStaffResolver currentStaff;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentStaff);
    }
}
