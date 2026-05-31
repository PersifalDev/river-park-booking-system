package ru.haritonenko.bookingservice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>("postgres:15.3")
                    .withDatabaseName("bookingServiceTestDb")
                    .withUsername("testBooking")
                    .withPassword("testService")
                    .withReuse(true);

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES_CONTAINER::getDriverClassName);

        registry.add("jwt.secret-key", () ->
                "YlQ3bU1oQ3lWZlZzV3Z6Z0dRZ2R0c1lXb3VtQ2ZyQ1JXQk1vT2h6R2t3aWJ0bHk3U0p6T1B6eVY2Z2x4Z0l3Yk5nY2l3TnZVQ1h2TnF3dEo");
        registry.add("jwt.lifetime", () -> "86400000");

        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.main.lazy-initialization", () -> "true");

        registry.add("spring.autoconfigure.exclude", () ->
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        );
    }



}
