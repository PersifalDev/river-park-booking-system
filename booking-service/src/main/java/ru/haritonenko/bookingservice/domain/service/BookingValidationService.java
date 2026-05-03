package ru.haritonenko.bookingservice.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.haritonenko.bookingservice.api.dto.BookingRequestDto;
import ru.haritonenko.bookingservice.external.client.catalog.CatalogServiceHttpClient;
import ru.haritonenko.commonlibs.exception.BookingGuestsOverloadedException;
import ru.haritonenko.commonlibs.exception.RoomCategoryNotFoundException;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingValidationService {

    private final CatalogServiceHttpClient catalogServiceHttpClient;

    public void validateBookingRequest(
            BookingRequestDto bookingRequest,
            Long userId
    ) {
        Long categoryIdFromRequest = bookingRequest.categoryId();

        if (isNull(categoryIdFromRequest)) {
            log.warn("Room category id from request has null value");
            throw new IllegalArgumentException("Category id from request is null");
        }

        if (isNull(userId)) {
            log.warn("User id from request has null value");
            throw new IllegalArgumentException("User id from request is null");
        }
        log.info("Validating booking request against external services: userId={}, categoryId={}",
                userId, categoryIdFromRequest);

        var category = catalogServiceHttpClient.getRoomCategoryById(categoryIdFromRequest);
        if (isNull(category)) {
            log.warn("Room category with id={} not found", categoryIdFromRequest);
            throw new RoomCategoryNotFoundException("Room category not found id=%s".formatted(categoryIdFromRequest));
        }

        if (bookingRequest.guests() > category.maxGuests()) {
            log.warn("Guests from booking request ({}) more than available ({}) for category with id={}",
                    bookingRequest.guests(),
                    category.maxGuests(),
                    categoryIdFromRequest
            );
            throw new BookingGuestsOverloadedException("More guests have been selected than are available");
        }
    }
}
