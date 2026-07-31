package ru.haritonenko.bookingservice.kafka.outbox.db;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.haritonenko.bookingservice.kafka.outbox.status.OutboxStatus;
import ru.haritonenko.bookingservice.kafka.outbox.status.OutboxEventKind;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "booking_outbox")
public class BookingOutboxEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_kind", nullable = false, length = 32)
    private OutboxEventKind eventKind;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = OutboxStatus.NEW;
        }
        if (eventKind == null) {
            eventKind = OutboxEventKind.BOOKING;
        }
        if (attempts == null) {
            attempts = 0;
        }
        if (nextAttemptAt == null) {
            nextAttemptAt = now;
        }
        createdAt = now;
    }
}
