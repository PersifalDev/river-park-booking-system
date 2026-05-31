package ru.haritonenko.bookingservice.rate.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.haritonenko.bookingservice.rate.SlidingWindowRateLimiter;
import ru.haritonenko.bookingservice.rate.config.BookingRateLimitProperties;
import ru.haritonenko.commonlibs.dto.error.ErrorMessageResponse;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;

import java.io.IOException;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class BookingRateLimitFilter extends OncePerRequestFilter {

    private final BookingRateLimitProperties properties;
    private final SlidingWindowRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return properties.getExcludedPathPrefixes().stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String key = buildKey(request);
        int limit = isCreateBookingRequest(request)
                ? properties.getMaxCreateBookingRequestsPerWindow()
                : properties.getMaxRequestsPerWindow();

        if (rateLimiter.tryAcquire(key, properties.getWindow(), limit)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorMessageResponse(
                "Too many requests",
                "Rate limit exceeded",
                OffsetDateTime.now().toString()
        ));
    }

    private boolean isCreateBookingRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && "/booking".equals(request.getRequestURI());
    }

    private String buildKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser user && user.id() != null) {
            return "user:%s:%s:%s".formatted(user.id(), request.getMethod(), request.getRequestURI());
        }
        return "ip:%s:%s:%s".formatted(clientIp(request), request.getMethod(), request.getRequestURI());
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
