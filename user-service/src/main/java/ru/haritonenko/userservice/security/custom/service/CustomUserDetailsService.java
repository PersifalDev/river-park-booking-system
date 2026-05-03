package ru.haritonenko.userservice.security.custom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import ru.haritonenko.userservice.domain.db.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Loading user by login: {}", username);
        var user = userRepository.findByLogin(username)
                .orElseThrow(() -> {
                    log.warn("Error while searching for user by login: {}", username);
                    return new UsernameNotFoundException("User not found by login: %s".formatted(username));
                });
        log.info("User with login: {} was successfully loaded", username);
        return User.withUsername(username)
                .password(user.getKey())
                .authorities(String.valueOf(user.getUserRole()))
                .build();
    }
}
