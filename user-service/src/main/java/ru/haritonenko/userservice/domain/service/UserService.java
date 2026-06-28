package ru.haritonenko.userservice.domain.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.userservice.api.dto.UserRegistration;
import ru.haritonenko.userservice.consent.service.UserConsentService;
import ru.haritonenko.userservice.domain.User;
import ru.haritonenko.userservice.domain.UserRole;
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
    private final UserConsentService userConsentService;

    @Cacheable(value = "users", key = "'id:' + #id")
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

    @Transactional(readOnly = true)
    public Page<User> getUsers(Pageable pageable) {
        log.info("Getting users page: pageNumber={}, pageSize={}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Transactional
    @Caching(put = {
            @CachePut(value = "users", key = "'id:' + #result.id()"),
            @CachePut(value = "users", key = "'login:' + #result.login()")
    })
    public User register(UserRegistration userFromRegistration) {

        if(isNull(userFromRegistration)){
            log.warn("Registration failed: registration payload is null");
            throw new IllegalArgumentException("Registration can't be null");
        }

        log.info("User registration started for login: {}", userFromRegistration.login());

        if (userRepository.existsByLogin(userFromRegistration.login())) {
            log.info("User already registered, login={}", userFromRegistration.login());
            throw new UserAlreadyRegisteredException("This user has already registered");
        }
        var hashedPass = passwordEncoder.encode(userFromRegistration.key());
        var userToSave = mapper.toEntity(userFromRegistration, hashedPass);
        var savedUserEntity = userRepository.save(userToSave);
        userConsentService.recordRegistrationConsents(savedUserEntity);
        log.info("User successfully registered with id: {}, login: {}",
                savedUserEntity.getId(), savedUserEntity.getLogin());
        return mapper.toDomain(savedUserEntity);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", key = "'id:' + #userId"),
            @CacheEvict(value = "users", key = "'login:' + #result.login()")
    })
    public User updateUserRole(Long userId, UserRole role) {
        if (isNull(role)) {
            log.warn("Role update failed: role is null for userId={}", userId);
            throw new IllegalArgumentException("User role can't be null");
        }

        log.info("Updating user role: userId={}, role={}", userId, role);
        var userEntity = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Role update failed: user not found by id={}", userId);
                    return new UserNotFoundException("No found user by id = %s".formatted(userId));
                });

        userEntity.setUserRole(role);
        var savedUser = userRepository.save(userEntity);
        log.info("User role successfully updated: userId={}, role={}", userId, role);
        return mapper.toDomain(savedUser);
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
