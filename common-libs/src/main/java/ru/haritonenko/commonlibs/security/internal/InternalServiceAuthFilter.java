package ru.haritonenko.commonlibs.security.internal;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;

import java.io.IOException;

public class InternalServiceAuthFilter implements Filter {

    public static final String INTERNAL_SERVICE_TOKEN_HEADER = "X-Internal-Service-Token";

    private final Environment environment;

    public InternalServiceAuthFilter(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (!isInternalRequest(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        String expectedToken = environment.getProperty("app.security.internal-service.token", "");
        if (expectedToken.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        String actualToken = httpRequest.getHeader(INTERNAL_SERVICE_TOKEN_HEADER);
        if (!expectedToken.equals(actualToken)) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Internal service token is invalid");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isInternalRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path.startsWith("/api/v1/internal/");
    }
}
