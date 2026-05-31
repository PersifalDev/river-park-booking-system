package ru.haritonenko.bookingservice.cache.keys;

import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.api.dto.filter.BookingPageFilter;
import ru.haritonenko.bookingservice.api.dto.filter.BookingRequestSearchFilter;

import java.util.UUID;

@Component
public class BookingCacheKeys {

    public String bookingByUser(Long userId, UUID bookingId) {
        return userId + ":" + bookingId;
    }

    public String bookingPage(Long userId, BookingPageFilter pageFilter) {
        return userId
                + ":page=" + (pageFilter == null || pageFilter.getPageNumber() == null ? "default" : pageFilter.getPageNumber())
                + ":size=" + (pageFilter == null || pageFilter.getPageSize() == null ? "default" : pageFilter.getPageSize());
    }

    public String bookingSearchPage(
            Long userId,
            BookingRequestSearchFilter filter,
            BookingPageFilter pageFilter
    ) {
        return userId
                + ":status=" + (filter == null ? "null" : filter.status())
                + ":active=" + (filter == null ? "null" : filter.active())
                + ":adultCount=" + (filter == null ? "null" : filter.adultCount())
                + ":childrenCount=" + (filter == null ? "null" : filter.childrenCount())
                + ":checkInDate=" + (filter == null ? "null" : filter.checkInDate())
                + ":checkOutDate=" + (filter == null ? "null" : filter.checkOutDate())
                + ":page=" + (pageFilter == null || pageFilter.getPageNumber() == null ? "default" : pageFilter.getPageNumber())
                + ":size=" + (pageFilter == null || pageFilter.getPageSize() == null ? "default" : pageFilter.getPageSize());
    }
}
