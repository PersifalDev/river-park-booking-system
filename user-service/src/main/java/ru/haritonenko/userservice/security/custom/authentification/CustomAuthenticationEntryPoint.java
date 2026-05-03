package ru.haritonenko.userservice.security.custom.authentification;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.haritonenko.commonlibs.dto.error.ErrorMessageResponse;


import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        log.warn("Handling authentication exception", authException);
        var messageResponse = new ErrorMessageResponse(
                "Failed to authenticate",
                authException.getMessage(),
                LocalDateTime.now().toString()
        );

        var stringResponse = objectMapper.writeValueAsString(messageResponse);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write(stringResponse);
    }
}