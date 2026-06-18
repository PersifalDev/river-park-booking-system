package ru.haritonenko.userservice.api.controller.web_only;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.haritonenko.commonlibs.security.authorization.user.UserCredentials;
import ru.haritonenko.userservice.api.controller.UserController;
import ru.haritonenko.userservice.api.dto.UserRegistration;
import ru.haritonenko.userservice.domain.User;
import ru.haritonenko.userservice.domain.UserRole;
import ru.haritonenko.userservice.domain.service.UserService;
import ru.haritonenko.userservice.security.jwt.manager.JwtTokenManager;
import ru.haritonenko.userservice.security.service.AuthenticationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtTokenManager jwtTokenManager;

    @Test
    void shouldGetUserById() throws Exception {
        when(userService.getUserById(1L)).thenReturn(new User(1L, "watson", UserRole.USER));

        mockMvc.perform(get("/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.login").value("watson"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(userService).getUserById(1L);
    }

    @Test
    void shouldRegisterUser() throws Exception {
        UserRegistration request = new UserRegistration("watson", "secret");
        when(userService.register(any(UserRegistration.class))).thenReturn(new User(1L, "watson", UserRole.USER));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.login").value("watson"));

        verify(userService).register(any(UserRegistration.class));
    }

    @Test
    void shouldAuthenticateUser() throws Exception {
        UserCredentials request = new UserCredentials("watson", "secret");
        when(authenticationService.authenticate(any(UserCredentials.class))).thenReturn("jwt-token");

        mockMvc.perform(post("/users/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").value("jwt-token"));

        verify(authenticationService).authenticate(any(UserCredentials.class));
    }

    @Test
    void shouldRejectInvalidRegistrationPayload() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
