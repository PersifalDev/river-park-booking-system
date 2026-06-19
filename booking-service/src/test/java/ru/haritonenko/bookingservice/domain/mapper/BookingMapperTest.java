package ru.haritonenko.bookingservice.domain.mapper;

import org.junit.jupiter.api.Test;
import ru.haritonenko.bookingservice.api.dto.BookingResponseDto;
import ru.haritonenko.bookingservice.domain.Booking;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookingMapperTest {

    private final BookingToDomainMapper domainMapper = new BookingToDomainMapperImpl();
    private final BookingToResponseDtoMapper responseMapper = new BookingToResponseDtoMapperImpl();

    @Test
    void shouldMapEntityToDomain() {
        BookingEntity entity = entity();

        Booking booking = domainMapper.toDomain(entity);

        assertEquals(entity.getId(), booking.id());
        assertEquals(entity.getBookingCode(), booking.bookingCode());
        assertEquals(entity.getRoomNumberSnapshot(), booking.roomNumberSnapshot());
        assertEquals(entity.getStatus(), booking.status());
        assertEquals(entity.getAdultCount(), booking.adultCount());
    }

    @Test
    void shouldMapDomainToResponseDto() {
        Booking booking = domainMapper.toDomain(entity());

        BookingResponseDto dto = responseMapper.toDto(booking);

        assertEquals(booking.id(), dto.id());
        assertEquals(booking.bookingCode(), dto.bookingCode());
        assertEquals(booking.roomNumberSnapshot(), dto.roomNumberSnapshot());
        assertEquals(booking.status(), dto.status());
        assertEquals(booking.priceAmount(), dto.priceAmount());
    }

    @Test
    void shouldReturnNullForNullInputs() {
        assertNull(domainMapper.toDomain(null));
        assertNull(responseMapper.toDto(null));
    }

    private BookingEntity entity() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        OffsetDateTime now = OffsetDateTime.now();
        return BookingEntity.builder()
                .id(UUID.randomUUID())
                .userId(10L)
                .roomCategoryId(1L)
                .roomNumberSnapshot("301")
                .bookingCode("BK-TEST")
                .guests(2)
                .adultCount(2)
                .childrenCount(0)
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(1))
                .priceAmount(BigDecimal.valueOf(5000))
                .holdExpiresAt(now.plusMinutes(15))
                .hasPromo(false)
                .status(BookingStatus.HOLD)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
