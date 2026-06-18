package ru.haritonenko.userservice.api.controller.all_context;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.haritonenko.userservice.domain.User;
import ru.haritonenko.userservice.domain.UserRole;
import ru.haritonenko.userservice.domain.service.UserService;
import ru.haritonenko.userservice.security.jwt.manager.JwtTokenManager;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-controller-context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.liquibase.enabled=false"
})
@AutoConfigureMockMvc(addFilters = false)
class UserControllerAllContextTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenManager jwtTokenManager;

    @Test
    void shouldLoadApplicationContextAndGetUserById() throws Exception {
        when(userService.getUserById(1L)).thenReturn(new User(1L, "watson", UserRole.USER));

        mockMvc.perform(get("/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("watson"));
    }
}
