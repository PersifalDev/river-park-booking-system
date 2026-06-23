package ru.haritonenko.catalogservice.rate.filter;

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
import ru.haritonenko.catalogservice.rate.SlidingWindowRateLimiter;
import ru.haritonenko.catalogservice.rate.config.CatalogRateLimitProperties;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;

import java.io.IOException;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class CatalogRateLimitFilter extends OncePerRequestFilter {

    private final CatalogRateLimitProperties properties;
    private final SlidingWindowRateLimiter rateLimiter;

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
        int limit = isSearchRequest(request)
                ? properties.getMaxSearchRequestsPerWindow()
                : properties.getMaxRequestsPerWindow();

        if (rateLimiter.tryAcquire(key, properties.getWindow(), limit)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"message":"Too many requests","detailedMessage":"Catalog rate limit exceeded","timestamp":"%s"}\
                """.formatted(OffsetDateTime.now()));
    }

    private boolean isSearchRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/api/v1/catalog/rooms/search".equals(request.getRequestURI());
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
