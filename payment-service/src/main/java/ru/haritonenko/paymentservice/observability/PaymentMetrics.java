package ru.haritonenko.paymentservice.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import ru.haritonenko.paymentservice.domain.status.PaymentStatus;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

@Component
public class PaymentMetrics {

    private final Map<PaymentStatus, Counter> paymentEvents;
    private final Counter paymentFailures;

    public PaymentMetrics(MeterRegistry meterRegistry) {
        paymentEvents = new EnumMap<>(PaymentStatus.class);
        for (PaymentStatus status : PaymentStatus.values()) {
            paymentEvents.put(status, Counter.builder("payment_events_total")
                    .description("Payment lifecycle events")
                    .tag("status", tag(status.name()))
                    .register(meterRegistry));
        }
        paymentFailures = Counter.builder("payment_failures_total")
                .description("Payment processing failures")
                .register(meterRegistry);
    }

    public void record(PaymentStatus status) {
        Counter counter = paymentEvents.get(status);
        if (counter != null) {
            counter.increment();
        }
    }

    public void recordFailure() {
        paymentFailures.increment();
    }

    private String tag(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
