package com.hotelbooking.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class TraceFilter extends OncePerRequestFilter {
    private static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain chain) throws ServletException, IOException {
        String previous = MDC.get("traceId");
        String incoming = request.getHeader("X-Trace-Id");
        String trace = incoming != null && incoming.matches("[a-zA-Z0-9-]{8,64}") ? incoming : UUID.randomUUID().toString();
        MDC.put("traceId", trace);
        response.setHeader("X-Trace-Id", trace);
        boolean api = request.getRequestURI().startsWith("/api/");
        String method = METHODS.contains(request.getMethod()) ? request.getMethod() : "OTHER";
        String scope = request.getRequestURI().startsWith("/api/auth/") ? "auth"
            : request.getRequestURI().startsWith("/api/crm/") ? "crm" : "public";
        long started = System.nanoTime();
        boolean failed = false;
        if (api) {
            log.info("event=http.request.started method={} scope={}", method, scope);
        }
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException e) {
            failed = true;
            throw e;
        } finally {
            if (api) {
                // Match patterns contain placeholders, never guest emails, tag values or query parameters.
                Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                log.info("event=http.request.completed method={} scope={} route={} status={} durationMs={}",
                    method, scope,
                    pattern == null ? "unmatched" : pattern, failed ? 500 : response.getStatus(),
                    (System.nanoTime() - started) / 1_000_000);
            }
            if (previous == null) {
                MDC.remove("traceId");
            } else {
                MDC.put("traceId", previous);
            }
        }
    }
}
