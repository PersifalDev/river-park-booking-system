package ru.haritonenko.notificationservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notification-service-context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.kafka.listener.auto-startup=false",
        "spring.liquibase.enabled=false"
})
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
