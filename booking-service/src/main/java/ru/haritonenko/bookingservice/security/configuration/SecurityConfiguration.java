package ru.haritonenko.bookingservice.security.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import ru.haritonenko.bookingservice.rate.filter.BookingRateLimitFilter;
import ru.haritonenko.bookingservice.security.custom.CustomAccessDeniedHandler;
import ru.haritonenko.bookingservice.security.custom.CustomAuthenticationEntryPoint;
import ru.haritonenko.bookingservice.security.jwt.filter.JwtTokenFilter;
import ru.haritonenko.commonlibs.security.authorization.role.PlatformRole;


@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtTokenFilter jwtTokenFilter;
    private final BookingRateLimitFilter bookingRateLimitFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Local username/password authentication is disabled");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error", "/error/**").permitAll()
                        .requestMatchers("/booking/admin/**").hasAnyAuthority(
                                PlatformRole.ADMIN_AUTHORITY,
                                PlatformRole.BOOKING_MANAGER_AUTHORITY
                        )
                        .requestMatchers("/booking/*/confirm", "/booking/inactive", "/booking/early").hasAnyAuthority(
                                PlatformRole.ADMIN_AUTHORITY,
                                PlatformRole.BOOKING_MANAGER_AUTHORITY
                        )
                        .requestMatchers("/booking/*/cancel").hasAnyAuthority(
                                PlatformRole.ADMIN_AUTHORITY,
                                PlatformRole.BOOKING_MANAGER_AUTHORITY,
                                PlatformRole.USER_AUTHORITY
                        )
                        .requestMatchers("/booking/completed").hasAnyAuthority(
                                PlatformRole.ADMIN_AUTHORITY,
                                PlatformRole.BOOKING_MANAGER_AUTHORITY
                        )
                        .requestMatchers("/booking/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtTokenFilter, AnonymousAuthenticationFilter.class)
                .addFilterAfter(bookingRateLimitFilter, JwtTokenFilter.class)
                .build();
    }
}
