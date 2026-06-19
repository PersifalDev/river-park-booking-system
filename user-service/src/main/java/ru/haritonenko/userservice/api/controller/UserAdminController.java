package ru.haritonenko.userservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.haritonenko.userservice.api.dto.UserRoleUpdateRequest;
import ru.haritonenko.userservice.config.AdminCapabilitiesProperties;
import ru.haritonenko.userservice.domain.User;
import ru.haritonenko.userservice.domain.UserRole;
import ru.haritonenko.userservice.domain.service.UserService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users/admin")
@RequiredArgsConstructor
@Tag(name = "User admin", description = "Admin operations for users and staff roles")
@SecurityRequirement(name = "bearerAuth")
public class UserAdminController {

    private final UserService userService;
    private final AdminCapabilitiesProperties adminCapabilitiesProperties;

    @GetMapping
    @Operation(summary = "Get users page")
    public ResponseEntity<Page<User>> getUsers(@PageableDefault(size = 20) Pageable pageable) {
        log.info("Admin request for getting users page");
        return ResponseEntity.ok(userService.getUsers(pageable));
    }

    @GetMapping("/roles")
    @Operation(summary = "Get assignable user roles")
    public ResponseEntity<List<UserRole>> getRoles() {
        log.info("Admin request for getting assignable user roles");
        return ResponseEntity.ok(UserRole.adminAssignableRoles());
    }

    @GetMapping("/capabilities")
    @Operation(summary = "Get admin capabilities by role")
    public ResponseEntity<Map<String, List<String>>> getCapabilities() {
        log.info("Admin request for getting capabilities by role");
        return ResponseEntity.ok(adminCapabilitiesProperties.getCapabilitiesByRole());
    }

    @PatchMapping("/{userId}/role")
    @Operation(summary = "Update user role")
    public ResponseEntity<User> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleUpdateRequest request
    ) {
        log.info("Admin request for updating user role: userId={}, role={}", userId, request.role());
        return ResponseEntity.ok(userService.updateUserRole(userId, request.role()));
    }
}
