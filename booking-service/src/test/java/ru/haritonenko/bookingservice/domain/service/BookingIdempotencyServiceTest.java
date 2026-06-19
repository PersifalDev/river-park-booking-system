package ru.haritonenko.bookingservice.domain.service;

import org.junit.jupiter.api.Test;
import ru.haritonenko.bookingservice.api.dto.BookingRequestDto;
import ru.haritonenko.bookingservice.domain.db.entity.BookingIdempotencyKeyEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingIdempotencyKeyRepository;
import ru.haritonenko.bookingservice.domain.exception.BookingIdempotencyConflictException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingIdempotencyServiceTest {

    private final BookingIdempotencyKeyRepository repository = mock(BookingIdempotencyKeyRepository.class);

    private final BookingIdempotencyService service = new BookingIdempotencyService(repository, Duration.ofHours(24));

    @Test
    void shouldReturnEmptyForBlankKey() {
        assertTrue(service.findExisting(1L, "  ", "hash").isEmpty());

        verify(repository, never()).findByUserIdAndIdempotencyKeyAndExpiresAtAfter(any(), any(), any());
    }

    @Test
    void shouldFindExistingForSameRequestHash() {
        BookingIdempotencyKeyEntity entity = BookingIdempotencyKeyEntity.builder()
                .userId(1L)
                .idempotencyKey("key")
                .requestHash("hash")
                .bookingId(UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build();
        when(repository.findByUserIdAndIdempotencyKeyAndExpiresAtAfter(eq(1L), eq("key"), any()))
                .thenReturn(Optional.of(entity));

        Optional<BookingIdempotencyKeyEntity> actual = service.findExisting(1L, " key ", "hash");

        assertTrue(actual.isPresent());
        assertEquals(entity.getBookingId(), actual.get().getBookingId());
    }

    @Test
    void shouldRejectSameKeyWithDifferentHash() {
        BookingIdempotencyKeyEntity entity = BookingIdempotencyKeyEntity.builder()
                .requestHash("old")
                .build();
        when(repository.findByUserIdAndIdempotencyKeyAndExpiresAtAfter(eq(1L), eq("key"), any()))
                .thenReturn(Optional.of(entity));

        assertThrows(BookingIdempotencyConflictException.class, () -> service.findExisting(1L, "key", "new"));
    }

    @Test
    void shouldHashPromoCodeCaseInsensitively() {
        BookingRequestDto lowerCasePromo = request(" promo10 ");
        BookingRequestDto upperCasePromo = request("PROMO10");

        assertEquals(service.hash(lowerCasePromo), service.hash(upperCasePromo));
    }

    @Test
    void shouldRememberNormalizedKey() {
        UUID bookingId = UUID.randomUUID();

        service.remember(1L, " key ", "hash", bookingId);

        verify(repository).save(argThat(entity ->
                entity.getUserId().equals(1L)
                        && entity.getIdempotencyKey().equals("key")
                        && entity.getRequestHash().equals("hash")
                        && entity.getBookingId().equals(bookingId)
                        && entity.getExpiresAt() != null
        ));
    }

    @Test
    void shouldSkipRememberForBlankKey() {
        service.remember(1L, " ", "hash", UUID.randomUUID());

        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectTooLongKey() {
        String tooLongKey = "x".repeat(129);

        assertFalse(tooLongKey.isBlank());
        assertThrows(BookingIdempotencyConflictException.class, () -> service.findExisting(1L, tooLongKey, "hash"));
    }

    private BookingRequestDto request(String promoCode) {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        return BookingRequestDto.builder()
                .categoryId(1L)
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(1))
                .guests(2)
                .adultCount(2)
                .childrenCount(0)
                .promoCode(promoCode)
                .build();
    }
}
