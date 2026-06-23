package ru.haritonenko.catalogservice.security.configuration;

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
import ru.haritonenko.catalogservice.rate.filter.CatalogRateLimitFilter;
import ru.haritonenko.catalogservice.security.jwt.filter.JwtTokenFilter;
import ru.haritonenko.commonlibs.security.authorization.role.PlatformRole;

@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtTokenFilter jwtTokenFilter;
    private final CatalogRateLimitFilter catalogRateLimitFilter;

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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/catalog/admin/**").hasAnyAuthority(
                                PlatformRole.ADMIN_AUTHORITY,
                                PlatformRole.CONTENT_MANAGER_AUTHORITY
                        )
                        .anyRequest().permitAll())
                .addFilterBefore(jwtTokenFilter, AnonymousAuthenticationFilter.class)
                .addFilterAfter(catalogRateLimitFilter, JwtTokenFilter.class)
                .build();
    }
}
