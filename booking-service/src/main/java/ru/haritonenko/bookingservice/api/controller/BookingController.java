package ru.haritonenko.bookingservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.haritonenko.bookingservice.api.dto.AvailableRoomSearchRequestDto;
import ru.haritonenko.bookingservice.api.dto.BookingRequestDto;
import ru.haritonenko.bookingservice.api.dto.BookingResponseDto;
import ru.haritonenko.bookingservice.api.dto.filter.BookingPageFilter;
import ru.haritonenko.bookingservice.api.dto.filter.BookingRequestSearchFilter;
import ru.haritonenko.bookingservice.domain.mapper.BookingToResponseDtoMapper;
import ru.haritonenko.bookingservice.domain.service.BookingService;
import ru.haritonenko.bookingservice.security.service.AuthenticationService;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;
import ru.haritonenko.commonlibs.utils.pages.PageResponse;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/booking")
@RequiredArgsConstructor
@Tag(name = "Booking", description = "Создание, поиск, подтверждение и отмена бронирований")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;
    private final AuthenticationService authenticationService;
    private final BookingToResponseDtoMapper mapper;

    @PostMapping
    @Operation(summary = "Создать бронь", description = "Создает черновик брони и запускает асинхронную цепочку проверки, расчета цены и HOLD inventory.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Бронь создана"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "409", description = "Конфликт бронирования или идемпотентности"),
            @ApiResponse(responseCode = "429", description = "Превышен rate limit")
    })
    public ResponseEntity<BookingResponseDto> createBooking(
            @Parameter(in = ParameterIn.HEADER, description = "Ключ идемпотентности для безопасного повтора запроса")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody BookingRequestDto bookingRequestDto
    ) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for creating booking for categoryId={} by user={}",
                bookingRequestDto.categoryId(),
                authUserId
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toDto(bookingService.createBooking(bookingRequestDto, authUserId, idempotencyKey)));
    }

    @PostMapping("/available/search")
    @Operation(summary = "Найти доступные категории номеров", description = "Возвращает категории, у которых есть свободные номера на выбранный период.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Доступные категории найдены"),
            @ApiResponse(responseCode = "400", description = "Некорректный фильтр")
    })
    public ResponseEntity<PageResponse<RoomCategoryResponseDto>> searchAvailableRoomCategories(
            @Valid @RequestBody AvailableRoomSearchRequestDto request,
            @Valid @ModelAttribute BookingPageFilter pageFilter
    ) {
        AuthUser user = getAuthenticatedUser();
        log.info("Request for searching available rooms by dates for user with id={}", user.id());
        return ResponseEntity.ok(bookingService.searchAvailableRoomCategories(request, pageFilter));
    }

    @PostMapping("/search")
    @Operation(summary = "Найти брони пользователя", description = "Поиск броней текущего пользователя по статусу, датам и составу гостей.")
    @ApiResponse(responseCode = "200", description = "Страница броней")
    public ResponseEntity<Page<BookingResponseDto>> searchBookingsByUserId(
            @Valid @RequestBody BookingRequestSearchFilter bookingFilter,
            @Valid @ModelAttribute BookingPageFilter pageFilter
    ) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for searching bookings by filter for user={}", authUserId);
        return ResponseEntity.ok(bookingService.findAllBookingsByFilterAndByUserId(authUserId, bookingFilter, pageFilter).map(mapper::toDto));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Получить бронь по UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Бронь найдена"),
            @ApiResponse(responseCode = "404", description = "Бронь не найдена")
    })
    public ResponseEntity<BookingResponseDto> getBookingByUuid(
            @Parameter(description = "UUID брони") @PathVariable UUID uuid
    ) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for getting booking with uuid={} for user={}", uuid, authUserId);
        return ResponseEntity.ok(mapper.toDto(bookingService.getBookingByUuidAndUserId(authUserId, uuid)));
    }

    @GetMapping
    @Operation(summary = "Получить активные брони", description = "Возвращает HOLD и CONFIRMED брони текущего пользователя.")
    @ApiResponse(responseCode = "200", description = "Страница активных броней")
    public ResponseEntity<Page<BookingResponseDto>> getAllActiveBookingsByUserId(
            @Valid @ModelAttribute BookingPageFilter pageFilter
    ) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for getting all active bookings for user={}", authUserId);
        return ResponseEntity.ok(bookingService.getAllActiveBookingsByUserId(authUserId, pageFilter).map(mapper::toDto));
    }

    @GetMapping("/inactive")
    @Operation(summary = "Получить несостоявшиеся брони", description = "Возвращает CANCELLED, EXPIRED и FAILED брони текущего пользователя.")
    @ApiResponse(responseCode = "200", description = "Страница несостоявшихся броней")
    public ResponseEntity<Page<BookingResponseDto>> getAllInactiveBookingsByUserId(
            @Valid @ModelAttribute BookingPageFilter pageFilter
    ) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for getting all inactive bookings for user={}", authUserId);
        return ResponseEntity.ok(bookingService.getAllInactiveBookingsByUserId(authUserId, pageFilter).map(mapper::toDto));
    }

    @GetMapping("/early")
    @Operation(summary = "Получить ранние состоявшиеся брони", description = "Возвращает CONFIRMED брони текущего пользователя с датой выезда старше месяца.")
    @ApiResponse(responseCode = "200", description = "Страница ранних состоявшихся броней")
    public ResponseEntity<Page<BookingResponseDto>> getAllEarlyCompletedBookingsByUserId(
            @Valid @ModelAttribute BookingPageFilter pageFilter
    ) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for getting all early completed bookings for user={}", authUserId);
        return ResponseEntity.ok(bookingService.getAllEarlyCompletedBookingsByUserId(authUserId, pageFilter).map(mapper::toDto));
    }

    @GetMapping("/history")
    @Operation(summary = "Получить историю бронирований", description = "Возвращает все брони текущего пользователя.")
    @ApiResponse(responseCode = "200", description = "Страница истории бронирований")
    public ResponseEntity<Page<BookingResponseDto>> getAllHistoryBookingsByUserId(
            @Valid @ModelAttribute BookingPageFilter pageFilter
    ) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for getting all booking history for user={}", authUserId);
        return ResponseEntity.ok(bookingService.getAllHistoryBookingsByUserId(authUserId, pageFilter).map(mapper::toDto));
    }

    @DeleteMapping("/inactive")
    @Operation(summary = "Очистить несостоявшиеся брони")
    @ApiResponse(responseCode = "204", description = "Несостоявшиеся брони текущего пользователя очищены")
    public ResponseEntity<Void> clearInactiveBookingsByUserId() {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for clearing inactive bookings for user={}", authUserId);
        bookingService.deleteInactiveBookingsByUserId(authUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/completed")
    @Operation(summary = "Очистить состоявшиеся брони")
    @ApiResponse(responseCode = "204", description = "Состоявшиеся брони текущего пользователя очищены")
    public ResponseEntity<Void> clearCompletedBookingsByUserId() {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for clearing completed bookings for user={}", authUserId);
        bookingService.deleteCompletedBookingsByUserId(authUserId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{uuid}/cancel")
    @Operation(summary = "Отменить бронь")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Бронь отменена"),
            @ApiResponse(responseCode = "404", description = "Бронь не найдена"),
            @ApiResponse(responseCode = "409", description = "Бронь нельзя отменить в текущем статусе")
    })
    public ResponseEntity<BookingResponseDto> cancelBookingByUuidAndUserId(
            @Parameter(description = "UUID брони") @PathVariable UUID uuid
    ) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for cancelling booking with uuid={} for user={}", uuid, authUserId);
        return ResponseEntity.ok(mapper.toDto(bookingService.cancelBookingByUuidAndUserId(uuid, authUserId)));
    }

    @PatchMapping("/{uuid}/confirm")
    @Operation(summary = "Подтвердить бронь")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Бронь подтверждена"),
            @ApiResponse(responseCode = "404", description = "Бронь не найдена"),
            @ApiResponse(responseCode = "409", description = "Бронь нельзя подтвердить в текущем статусе")
    })
    public ResponseEntity<BookingResponseDto> confirmBookingByUuidAndUserId(
            @Parameter(description = "UUID брони") @PathVariable UUID uuid
    ) {
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
