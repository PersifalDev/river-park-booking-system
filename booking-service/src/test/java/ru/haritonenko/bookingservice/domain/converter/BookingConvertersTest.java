package ru.haritonenko.bookingservice.domain.converter;

import org.junit.jupiter.api.Test;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import ru.haritonenko.bookingservice.tasks.domain.async.converter.AsyncBookingStatusConverter;
import ru.haritonenko.bookingservice.tasks.domain.async.converter.ProcessingStepConverter;
import ru.haritonenko.bookingservice.tasks.domain.async.status.AsyncBookingTaskStatus;
import ru.haritonenko.bookingservice.tasks.domain.async.status.ProcessingStep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookingConvertersTest {

    @Test
    void shouldConvertBookingStatus() {
        BookingStatusConverter converter = new BookingStatusConverter();

        assertEquals(BookingStatus.HOLD.getCode(), converter.convertToDatabaseColumn(BookingStatus.HOLD));
        assertEquals(BookingStatus.HOLD, converter.convertToEntityAttribute(BookingStatus.HOLD.getCode()));
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void shouldConvertAsyncTaskStatus() {
        AsyncBookingStatusConverter converter = new AsyncBookingStatusConverter();

        assertEquals(AsyncBookingTaskStatus.FAILED_RETRYABLE.getCode(),
                converter.convertToDatabaseColumn(AsyncBookingTaskStatus.FAILED_RETRYABLE));
        assertEquals(AsyncBookingTaskStatus.FAILED_RETRYABLE,
                converter.convertToEntityAttribute(AsyncBookingTaskStatus.FAILED_RETRYABLE.getCode()));
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void shouldConvertProcessingStep() {
        ProcessingStepConverter converter = new ProcessingStepConverter();

        assertEquals(ProcessingStep.CREATE_HOLD.getCode(), converter.convertToDatabaseColumn(ProcessingStep.CREATE_HOLD));
        assertEquals(ProcessingStep.CREATE_HOLD, converter.convertToEntityAttribute(ProcessingStep.CREATE_HOLD.getCode()));
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
