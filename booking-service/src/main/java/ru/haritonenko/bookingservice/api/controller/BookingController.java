package ru.haritonenko.bookingservice.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.haritonenko.bookingservice.api.dto.AvailableRoomSearchRequestDto;
import ru.haritonenko.bookingservice.api.dto.BookingRequestDto;
import ru.haritonenko.bookingservice.api.dto.BookingResponseDto;
import ru.haritonenko.bookingservice.api.dto.filter.BookingPageFilter;
import ru.haritonenko.bookingservice.api.dto.filter.BookingRequestSearchFilter;
import ru.haritonenko.bookingservice.domain.mapper.BookingToResponseDtoMapper;
import ru.haritonenko.bookingservice.domain.service.BookingService;
import ru.haritonenko.bookingservice.security.service.AuthenticationService;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;

import java.util.UUID;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.utils.pages.PageResponse;

@Slf4j
@Validated
@RestController
@RequestMapping("/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final AuthenticationService authenticationService;
    private final BookingToResponseDtoMapper mapper;

    @PostMapping
    public ResponseEntity<BookingResponseDto> createBooking(
            @Valid @RequestBody BookingRequestDto bookingRequestDto
    ) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for creating booking for categoryId={} by user={}", bookingRequestDto.categoryId(), authUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toDto(bookingService.createBooking(bookingRequestDto, authUserId)));
    }

    @PostMapping("/available/search")
    public ResponseEntity<PageResponse<RoomCategoryResponseDto>> searchAvailableRoomCategories(
            @Valid @RequestBody AvailableRoomSearchRequestDto request,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "1") int pageSize
    ) {
        getAuthenticatedUser();
        log.info("Request for searching available rooms by dates");
        return ResponseEntity.ok(bookingService.searchAvailableRoomCategories(request, pageNumber, pageSize));
    }

    @PostMapping("/search")
    public ResponseEntity<Page<BookingResponseDto>> searchBookingsByUserId(
            @Valid @RequestBody BookingRequestSearchFilter bookingFilter,
            @Valid @ModelAttribute BookingPageFilter pageFilter
    ) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for searching bookings by filter for user={}", authUserId);
        return ResponseEntity.ok(bookingService.findAllBookingsByFilterAndByUserId(authUserId, bookingFilter, pageFilter).map(mapper::toDto));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<BookingResponseDto> getBookingByUuid(@PathVariable UUID uuid) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for getting booking with uuid={} for user={}", uuid, authUserId);
        return ResponseEntity.ok(mapper.toDto(bookingService.getBookingByUuidAndUserId(authUserId, uuid)));
    }

    @GetMapping
    public ResponseEntity<Page<BookingResponseDto>> getAllActiveBookingsByUserId(
            @Valid @ModelAttribute BookingPageFilter pageFilter
    ) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for getting all active bookings for user={}", authUserId);
        return ResponseEntity.ok(bookingService.getAllActiveBookingsByUserId(authUserId, pageFilter).map(mapper::toDto));
    }

    @PatchMapping("/{uuid}/cancel")
    public ResponseEntity<BookingResponseDto> cancelBookingByUuidAndUserId(@PathVariable UUID uuid) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for cancelling booking with uuid={} for user={}", uuid, authUserId);
        return ResponseEntity.ok(mapper.toDto(bookingService.cancelBookingByUuidAndUserId(uuid, authUserId)));
    }

    @PatchMapping("/{uuid}/confirm")
    public ResponseEntity<BookingResponseDto> confirmBookingByUuidAndUserId(@PathVariable UUID uuid) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for confirming booking with uuid={} for user={}", uuid, authUserId);
        return ResponseEntity.ok(mapper.toDto(bookingService.confirmBookingByUuidAndUserId(uuid, authUserId)));
    }

    private AuthUser getAuthenticatedUser() {
        AuthUser authUser = authenticationService.getCurrentAuthenticatedUser();
        log.info("Got authenticated user with id={}", authUser.id());
        return authUser;
    }
}
