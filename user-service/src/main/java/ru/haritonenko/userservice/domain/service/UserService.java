package ru.haritonenko.userservice.domain.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.userservice.api.dto.UserRegistration;
import ru.haritonenko.userservice.domain.User;
import ru.haritonenko.userservice.domain.db.repository.UserRepository;
import ru.haritonenko.userservice.domain.exception.UserAlreadyRegisteredException;
import ru.haritonenko.userservice.domain.exception.UserNotFoundException;
import ru.haritonenko.userservice.domain.mapper.UserEntityMapper;


import static java.util.Objects.isNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserEntityMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        log.info("Getting user by id: {}", id);
        var foundUser = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Error while getting user by id");
                    return new UserNotFoundException("No found user by id = %s".formatted(id));
                });
        log.info("User was successfully found by id: {}", id);
        return mapper.toDomain(foundUser);
    }

    @Transactional
    public User register(UserRegistration userFromRegistration) {

        if(isNull(userFromRegistration)){
            log.warn("Registration failed: registration payload is null");
            throw new IllegalArgumentException("Registration can't be null");
        }

        log.info("User registration started for login: {}", userFromRegistration.login());

        if (userRepository.existsByLogin(userFromRegistration.login())) {
            log.warn("Error while registering user");
            throw new UserAlreadyRegisteredException("This user has already registered");
        }
        var hashedPass = passwordEncoder.encode(userFromRegistration.key());
        var userToSave = mapper.toEntity(userFromRegistration, hashedPass);
        var savedUserEntity = userRepository.save(userToSave);
        log.info("User successfully registered with id: {}, login: {}",
                savedUserEntity.getId(), savedUserEntity.getLogin());
        return mapper.toDomain(savedUserEntity);
    }

    @Cacheable(value = "users", key = "'login:' + #login")
    @Transactional(readOnly = true)
    public User findByLogin(String login) {
        log.info("Searching for user by login: {}", login);
        var foundUser = userRepository.findByLogin(login)
                .orElseThrow(() -> {
                    log.warn("Error while finding user by login");
                    return new UserNotFoundException("User not found");
                });
        log.info("User was successfully found by login: {}", login);
        return mapper.toDomain(foundUser);
    }
}