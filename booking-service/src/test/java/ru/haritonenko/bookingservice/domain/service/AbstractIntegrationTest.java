package ru.haritonenko.bookingservice.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.haritonenko.bookingservice.lock.RedisDistributedLockService;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @MockitoBean
    protected RedisDistributedLockService redisDistributedLockService;

    @BeforeEach
    void runDistributedLockActionsInline() {
        doAnswer(invocation -> {
            Supplier<?> action = invocation.getArgument(1);
            return action.get();
        }).when(redisDistributedLockService).execute(anyString(), any(Supplier.class));

        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(1);
            action.run();
            return null;
        }).when(redisDistributedLockService).execute(anyString(), any(Runnable.class));
    }

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

        registry.add("spring.liquibase.enabled", () -> "true");
    }
}
