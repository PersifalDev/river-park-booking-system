package ru.haritonenko.userservice.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.haritonenko.commonlibs.security.authorization.user.UserCredentials;
import ru.haritonenko.userservice.api.dto.UserRegistration;
import ru.haritonenko.userservice.domain.User;
import ru.haritonenko.userservice.domain.service.UserService;
import ru.haritonenko.userservice.security.jwt.response.JwtResponse;
import ru.haritonenko.userservice.security.service.AuthenticationService;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        log.info("Request for getting user by id={}", id);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<User> register(@Valid @RequestBody UserRegistration userRegistration) {
        log.info("Request for registering user with login={}", userRegistration.login());
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(userRegistration));
    }

    @PostMapping("/auth")
    public ResponseEntity<JwtResponse> authenticate(@Valid @RequestBody UserCredentials userCredentials) {
        log.info("Request for authenticating user with login={}", userCredentials.login());
        return ResponseEntity.ok(new JwtResponse(authenticationService.authenticate(userCredentials)));
    }
}
