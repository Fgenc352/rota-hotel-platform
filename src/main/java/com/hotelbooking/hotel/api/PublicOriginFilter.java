package com.hotelbooking.hotel.api;

import com.hotelbooking.hotel.application.port.HotelCatalog;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class PublicOriginFilter extends OncePerRequestFilter {
    private final HotelCatalog hotels;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain chain) throws ServletException,
    IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/public/hotels/")) {
            String key = path.substring("/api/public/hotels/".length()).split("/")[0];
            String origin = request.getHeader("Origin");
            // Same-origin requests through the web proxy need no CORS header.
            String forwarded = request.getHeader("X-Forwarded-Host");
            String proto = request.getHeader("X-Forwarded-Proto");
            String own = (proto == null?request.getScheme():proto)+"://"+(forwarded == null?request.getHeader("Host"):forwarded);
            if (origin != null && !origin.equals(own)) {
                if (!hotels.allowsOrigin(key, origin)) {
                    response.setStatus(403);
                    return;
                }
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Vary", "Origin");
                response.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
                response.setHeader("Access-Control-Allow-Headers", "Content-Type,Idempotency-Key,X-Trace-Id");
            }
            if (request.getMethod().equals("OPTIONS")) {
                response.setStatus(204);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
