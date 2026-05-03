package ru.haritonenko.bookingservice.tasks.domain.async.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.haritonenko.bookingservice.tasks.domain.async.converter.AsyncBookingStatusConverter;
import ru.haritonenko.bookingservice.tasks.domain.async.converter.ProcessingStepConverter;
import ru.haritonenko.bookingservice.tasks.domain.async.status.AsyncBookingTaskStatus;
import ru.haritonenko.bookingservice.tasks.domain.async.status.ProcessingStep;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "async_booking_task")
public class AsyncBookingTaskEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message="Booking id can not be null")
    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @NotNull(message="Task status can not be null")
    @Column(name = "task_status", nullable = false)
    @Convert(converter = AsyncBookingStatusConverter.class)
    private AsyncBookingTaskStatus status;

    @NotNull(message="Processing step can not be null")
    @Column(name = "processing_step", nullable = false)
    @Convert(converter = ProcessingStepConverter.class)
    private ProcessingStep processingStep;

    @NotNull(message="Count of attempts can not be null")
    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @NotNull(message="Time of creation can not be null")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @NotNull(message="Time of update can not be null")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (attempts == null) {
            attempts = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
