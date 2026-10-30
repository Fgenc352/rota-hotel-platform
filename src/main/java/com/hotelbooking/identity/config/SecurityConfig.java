package com.hotelbooking.identity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http,
        @Value("${server.servlet.session.cookie.name:JSESSIONID}") String sessionCookieName) throws Exception {
        var contexts = new HttpSessionSecurityContextRepository();
        return http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/public/**")
            // Explicit Basic clients (Postman/Swagger) do not rely on ambient cookies.
            .ignoringRequestMatchers(request -> {
            String authorization = request.getHeader("Authorization");
            return authorization != null && authorization.startsWith("Basic ");
        }))
            .securityContext(context -> context.securityContextRepository(contexts))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .requestCache(cache -> cache.disable())
            .logout(logout -> logout.logoutUrl("/api/auth/logout")
            .invalidateHttpSession(true)
            .deleteCookies(sessionCookieName)
            .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
            .authorizeHttpRequests(access -> access
            .requestMatchers("/api/public/**", "/actuator/health/**").permitAll()
            .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**",
            "/v3/api-docs.yaml").permitAll()
            .requestMatchers("/api/crm/**", "/api/auth/me").hasRole("STAFF")
            .anyRequest().denyAll())
            .httpBasic(basic -> basic.securityContextRepository(contexts).authenticationEntryPoint((request, response,
            error) -> {
            response.setStatus(401);
            response.setContentType("application/problem+json");
            response.getWriter().write("""
                    {"status":401,"code":"UNAUTHENTICATED","detail":"Kullanıcı adı veya parola yanlış; oturumunuz yoksa yeniden giriş yapın."}
                    """.strip());
        }))
            .build();
    }
}
