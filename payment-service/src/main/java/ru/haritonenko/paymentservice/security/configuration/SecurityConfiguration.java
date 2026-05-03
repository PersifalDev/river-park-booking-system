package ru.haritonenko.paymentservice.security.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import ru.haritonenko.paymentservice.security.custom.CustomAccessDeniedHandler;
import ru.haritonenko.paymentservice.security.custom.CustomAuthenticationEntryPoint;
import ru.haritonenko.paymentservice.security.jwt.filter.JwtTokenFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtTokenFilter jwtTokenFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(customAuthenticationEntryPoint).accessDeniedHandler(customAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/error", "/error/**").permitAll().requestMatchers("/payments/**").authenticated().anyRequest().permitAll())
                .addFilterBefore(jwtTokenFilter, AnonymousAuthenticationFilter.class)
                .build();
    }
}
