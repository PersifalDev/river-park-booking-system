package ru.haritonenko.notificationservice.security.jwt.manager;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtTokenManager {

    @Value("${jwt.secret-key}")
    private String keyString;

    private SecretKey key;

    @PostConstruct
    void init() {
        key = Keys.hmacShaKeyFor(keyString.getBytes(StandardCharsets.UTF_8));
    }

    public AuthUser getAuthUserFromToken(String jwt) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(jwt).getPayload();
        return AuthUser.builder().id(claims.get("userId", Long.class)).login(claims.getSubject()).role(claims.get("role", String.class)).build();
    }
}
