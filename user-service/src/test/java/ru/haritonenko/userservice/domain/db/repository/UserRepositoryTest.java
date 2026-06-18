package ru.haritonenko.userservice.domain.db.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import ru.haritonenko.userservice.domain.UserRole;
import ru.haritonenko.userservice.domain.db.entity.UserEntity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @Test
    void shouldFindUserByLogin() {
        repository.saveAndFlush(UserEntity.builder()
                .login("watson")
                .key("hashed-password")
                .userRole(UserRole.USER)
                .build());

        assertTrue(repository.existsByLogin("watson"));
        assertTrue(repository.findByLogin("watson").isPresent());
        assertFalse(repository.existsByLogin("missing"));
        assertTrue(repository.findByLogin("missing").isEmpty());
    }
}
