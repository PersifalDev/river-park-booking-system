package ru.haritonenko.userservice.api.controller.web_only;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.haritonenko.userservice.api.controller.UserAdminController;
import ru.haritonenko.userservice.config.AdminCapabilitiesProperties;
import ru.haritonenko.userservice.domain.User;
import ru.haritonenko.userservice.domain.UserRole;
import ru.haritonenko.userservice.domain.service.UserService;
import ru.haritonenko.userservice.security.jwt.manager.JwtTokenManager;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserAdminControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenManager jwtTokenManager;

    @MockitoBean
    private AdminCapabilitiesProperties adminCapabilitiesProperties;

    @Test
    void shouldGetAssignableRoles() throws Exception {
        mockMvc.perform(get("/users/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("ADMIN"))
                .andExpect(jsonPath("$[1]").value("BOOKING_MANAGER"))
                .andExpect(jsonPath("$[2]").value("CONTENT_MANAGER"));
    }

    @Test
    void shouldGetAdminCapabilities() throws Exception {
        when(adminCapabilitiesProperties.getCapabilitiesByRole()).thenReturn(Map.of(
                "ADMIN", List.of("USERS", "ROLES"),
                "BOOKING_MANAGER", List.of("MANUAL_BOOKINGS")
        ));

        mockMvc.perform(get("/users/admin/capabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ADMIN[0]").value("USERS"))
                .andExpect(jsonPath("$.BOOKING_MANAGER[0]").value("MANUAL_BOOKINGS"));
    }

    @Test
    void shouldGetUsersPage() throws Exception {
        when(userService.getUsers(any())).thenReturn(new PageImpl<>(List.of(
                new User(1L, "admin", UserRole.ADMIN),
                new User(2L, "manager", UserRole.BOOKING_MANAGER)
        )));

        mockMvc.perform(get("/users/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.content[1].role").value("BOOKING_MANAGER"));

        verify(userService).getUsers(any());
    }

    @Test
    void shouldUpdateUserRole() throws Exception {
        when(userService.updateUserRole(2L, UserRole.CONTENT_MANAGER))
                .thenReturn(new User(2L, "content", UserRole.CONTENT_MANAGER));

        mockMvc.perform(patch("/users/admin/{userId}/role", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "CONTENT_MANAGER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CONTENT_MANAGER"));

        verify(userService).updateUserRole(2L, UserRole.CONTENT_MANAGER);
    }
}
