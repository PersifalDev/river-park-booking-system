package ru.haritonenko.paymentservice.domain.db.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import ru.haritonenko.paymentservice.domain.db.entity.PaymentEntity;
import ru.haritonenko.paymentservice.domain.status.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PaymentEntityRepositoryTest {

    @Autowired
    private PaymentEntityRepository repository;

    @Test
    void shouldFindPaymentByBookingId() {
        UUID bookingId = UUID.randomUUID();
        repository.saveAndFlush(payment(bookingId, 10L));

        assertTrue(repository.findByBookingId(bookingId).isPresent());
        assertTrue(repository.findByBookingId(UUID.randomUUID()).isEmpty());
    }

    @Test
    void shouldFindPaymentsOnlyForRequestedUser() {
        repository.save(payment(UUID.randomUUID(), 10L));
        repository.save(payment(UUID.randomUUID(), 20L));
        repository.flush();

        var result = repository.findAllByUserId(10L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(10L, result.getContent().getFirst().getUserId());
    }

    private PaymentEntity payment(UUID bookingId, Long userId) {
        return PaymentEntity.builder()
                .bookingId(bookingId)
                .bookingCode("BK-" + bookingId)
                .userId(userId)
                .priceAmount(BigDecimal.valueOf(5000))
                .status(PaymentStatus.PENDING)
                .paymentMethod("PAY_ON_ARRIVAL")
                .paymentComment("Оплата при заселении")
                .contactPhone("+7 383 000-00-00")
                .paymentInstruction("Оплатите на стойке регистрации")
                .build();
    }
}
