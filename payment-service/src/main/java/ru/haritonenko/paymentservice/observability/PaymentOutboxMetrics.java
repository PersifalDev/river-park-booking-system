package ru.haritonenko.paymentservice.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import ru.haritonenko.paymentservice.outbox.PaymentOutboxRepository;
import ru.haritonenko.paymentservice.outbox.PaymentOutboxStatus;

import java.util.Locale;

@Component
public class PaymentOutboxMetrics {

    public PaymentOutboxMetrics(MeterRegistry meterRegistry, PaymentOutboxRepository repository) {
        for (PaymentOutboxStatus status : PaymentOutboxStatus.values()) {
            Gauge.builder("payment_outbox_backlog", repository, value -> value.countByStatus(status))
                    .description("Payment outbox records by status")
                    .tag("status", status.name().toLowerCase(Locale.ROOT))
                    .register(meterRegistry);
        }
    }
}
