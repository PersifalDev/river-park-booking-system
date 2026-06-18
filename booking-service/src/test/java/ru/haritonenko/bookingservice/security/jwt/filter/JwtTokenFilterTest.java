package ru.haritonenko.bookingservice.security.jwt.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.haritonenko.bookingservice.security.jwt.manager.JwtTokenManager;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtTokenFilterTest {

    private final JwtTokenManager jwtTokenManager = mock(JwtTokenManager.class);
    private final JwtTokenFilter filter = new JwtTokenFilter(jwtTokenManager);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateUserFromBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(jwtTokenManager.getAuthUserFromToken("jwt-token"))
                .thenReturn(AuthUser.builder()
                        .id(1L)
                        .login("user")
                        .role("USER")
                        .build());

        filter.doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(1L, ((AuthUser) authentication.getPrincipal()).id());
        assertEquals("USER", authentication.getAuthorities().iterator().next().getAuthority());
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldSkipInvalidTokenAndContinueChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer broken");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(jwtTokenManager.getAuthUserFromToken("broken")).thenThrow(new IllegalArgumentException("bad jwt"));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}
