package ru.haritonenko.bookingservice.domain.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.haritonenko.bookingservice.api.dto.AvailableRoomSearchRequestDto;
import ru.haritonenko.bookingservice.api.dto.BookingRequestDto;
import ru.haritonenko.bookingservice.api.dto.filter.BookingPageFilter;
import ru.haritonenko.bookingservice.api.dto.filter.BookingRequestSearchFilter;
import ru.haritonenko.bookingservice.config.validation.BookingValidationProperties;
import ru.haritonenko.bookingservice.cache.service.BookingCacheService;
import ru.haritonenko.bookingservice.domain.Booking;
import ru.haritonenko.bookingservice.domain.BookingCreationResult;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingEntityRepository;
import ru.haritonenko.bookingservice.domain.exception.BookingNotFoundException;
import ru.haritonenko.bookingservice.domain.exception.IllegalBookingStateException;
import ru.haritonenko.bookingservice.domain.mapper.BookingToDomainMapper;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import ru.haritonenko.bookingservice.external.client.catalog.CatalogServiceHttpClient;
import ru.haritonenko.bookingservice.kafka.outbox.service.BookingOutboxService;
import ru.haritonenko.bookingservice.kafka.producer.booking.sender.KafkaBookingEventSender;
import ru.haritonenko.bookingservice.kafka.producer.notification.sender.KafkaNotificationEventSender;
import ru.haritonenko.bookingservice.tasks.domain.async.db.entity.AsyncBookingTaskEntity;
import ru.haritonenko.bookingservice.tasks.domain.async.db.repository.AsyncBookingTaskEntityRepository;
import ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.AsyncBookingTaskDispatcher;
import ru.haritonenko.bookingservice.tasks.domain.async.status.AsyncBookingTaskStatus;
import ru.haritonenko.bookingservice.tasks.domain.async.status.ProcessingStep;
import ru.haritonenko.bookingservice.tasks.domain.exception.AsyncBookingTaskNotFoundException;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.NotificationKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.type.BookingEventType;
import ru.haritonenko.commonlibs.dto.kafka.event.type.NotificationEventType;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;
import ru.haritonenko.commonlibs.dto.kafka.payload.NotificationKafkaPayload;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.category.RoomCategorySearchRequestDto;
import ru.haritonenko.commonlibs.notification.NotificationStatus;
import ru.haritonenko.commonlibs.utils.pages.CommonPageable;
import ru.haritonenko.commonlibs.utils.pages.PageResponse;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(
            BookingStatus.HOLD,
            BookingStatus.CONFIRMED
    );
    private static final List<BookingStatus> INACTIVE_STATUSES = List.of(
            BookingStatus.CANCELLED,
            BookingStatus.EXPIRED,
            BookingStatus.FAILED
    );
    private static final String REVIEW_NOTIFICATION_TITLE = "Как прошло проживание?";

    private final BookingEntityRepository bookingRepository;
    private final AsyncBookingTaskEntityRepository taskRepository;
    private final AsyncBookingTaskDispatcher taskDispatcher;
    private final BookingToDomainMapper mapper;
    private final BookingCodeGenerator bookingCodeGenerator;
    private final BookingInventoryService bookingInventoryService;
    private final KafkaBookingEventSender bookingEventSender;
    private final KafkaNotificationEventSender notificationEventSender;
    private final BookingValidationService bookingValidationService;
    private final BookingValidationProperties bookingValidationProperties;
    private final CatalogServiceHttpClient catalogServiceHttpClient;
    private final BookingCacheService cacheService;
    private final BookingOutboxService bookingOutboxService;
    private final TransactionTemplate transactionTemplate;
    private final PromoCodeService promoCodeService;
    private final BookingIdempotencyService idempotencyService;

    @Value("${app.booking.default-page-number}")
    private int defaultPageNumber;

    @Value("${app.booking.default-page-size}")
    private int defaultPageSize;

    @Value("${app.booking.task.hold-ttl}")
    private Duration holdTtl;

    @Value("${app.booking.cleanup.retention-period}")
    private Duration cleanupRetentionPeriod;

    @Value("${app.booking.events.source}")
    private String sourceService;

    public Booking createBooking(BookingRequestDto bookingRequestDto, Long userId) {
        return createBooking(bookingRequestDto, userId, null);
    }

    public Booking createBooking(BookingRequestDto bookingRequestDto, Long userId, String idempotencyKey) {

        String requestHash = idempotencyService.hash(bookingRequestDto);
        Optional<Booking> existingBooking = idempotencyService.findExisting(userId, idempotencyKey, requestHash)
                .map(existing -> mapper.toDomain(findBookingEntity(existing.getBookingId())));
        if (existingBooking.isPresent()) {
            return existingBooking.get();
        }

        bookingValidationService.validateBookingRequest(bookingRequestDto, userId);

        BookingCreationResult bookingCreationResult = Optional.ofNullable(transactionTemplate.execute(status->{
            log.info("Creating booking draft: userId={}, categoryId={}, checkInDate={}, checkOutDate={}",
                    userId,
                    bookingRequestDto.categoryId(),
                    bookingRequestDto.checkInDate(),
                    bookingRequestDto.checkOutDate()
            );

            OffsetDateTime now = OffsetDateTime.now();
            BookingEntity savedBooking = bookingRepository.save(BookingEntity.builder()
                    .userId(userId)
                    .roomCategoryId(bookingRequestDto.categoryId())
                    .bookingCode(bookingCodeGenerator.generate())
                    .guests(bookingRequestDto.guests())
                    .adultCount(bookingRequestDto.adultCount())
                    .childrenCount(bookingRequestDto.childrenCount())
                    .checkInDate(bookingRequestDto.checkInDate())
                    .checkOutDate(bookingRequestDto.checkOutDate())
                    .priceAmount(BigDecimal.ONE)
                    .holdExpiresAt(now.plus(holdTtl))
                    .hasPromo(bookingRequestDto.promoCode() != null && !bookingRequestDto.promoCode().isBlank())
                    .appliedPromoCode(normalizePromoCode(bookingRequestDto.promoCode()))
                    .status(BookingStatus.CREATED)
                    .build());

            UUID savedBookingId = savedBooking.getId();
            idempotencyService.remember(userId, idempotencyKey, requestHash, savedBookingId);

            AsyncBookingTaskEntity task = taskRepository.save(AsyncBookingTaskEntity.builder()
                    .bookingId(savedBookingId)
                    .status(AsyncBookingTaskStatus.NEW)
                    .processingStep(ProcessingStep.VALIDATE_REQUEST)
                    .attempts(0)
                    .nextAttemptAt(now)
                    .build());

            log.info("Booking draft and async task created: bookingId={}, taskId={}", savedBookingId, task.getId());

            return new BookingCreationResult(savedBooking.getId(), task.getId());

        })).orElseThrow(() -> new IllegalBookingStateException("Booking creation transaction returned null result"));

        Long taskId = bookingCreationResult.taskId();
        UUID savedBookingId = bookingCreationResult.bookingId();

        AsyncBookingTaskEntity foundTask = taskRepository.findById(taskId)
                        .orElseThrow(()->{
                            log.warn("Task with id={} not found",taskId);
                            return new AsyncBookingTaskNotFoundException("Async task with id=%s not found".formatted(taskId));
                        });

        taskDispatcher.dispatchTask(foundTask);

        BookingEntity foundBooking = bookingRepository.findById(savedBookingId)
                .orElseThrow(() -> {
                    log.warn("Booking with id={} not found", savedBookingId);
                    return new BookingNotFoundException("Booking not found id=%s".formatted(savedBookingId));
                });

        cacheService.evictUserPages(userId);

        return mapper.toDomain(foundBooking);
    }

    @Cacheable(
            value = "bookingByUser",
            key = "@bookingCacheKeys.bookingByUser(#authUserId, #uuid)"
    )
    @Transactional(readOnly = true)
    public Booking getBookingByUuidAndUserId(Long authUserId, UUID uuid) {
        log.info("Getting booking by uuid and userId: uuid={}, userId={}", uuid, authUserId);
        return mapper.toDomain(findBookingEntityByIdAndUserId(uuid, authUserId));
    }

    @Cacheable(
            value = "bookingPages",
            key = "@bookingCacheService.registerBookingPageKey(" +
                    "#authUserId, " +
                    "@bookingCacheKeys.bookingPage(#authUserId, #pageFilter)" +
                    ")"
    )
    @Transactional(readOnly = true)
    public Page<Booking> getAllActiveBookingsByUserId(Long authUserId, BookingPageFilter pageFilter) {
        log.info("Getting all active bookings by userId={}", authUserId);

        Pageable pageable = CommonPageable.getPageable(pageFilter, defaultPageNumber, defaultPageSize);
        return bookingRepository.findAllByUserIdAndStatusIn(authUserId, ACTIVE_STATUSES, pageable)
                .map(mapper::toDomain);
    }

    @Cacheable(
            value = "bookingPages",
            key = "@bookingCacheService.registerBookingPageKey(" +
                    "#authUserId, " +
                    "'inactive:' + @bookingCacheKeys.bookingPage(#authUserId, #pageFilter)" +
                    ")"
    )
    @Transactional(readOnly = true)
    public Page<Booking> getAllInactiveBookingsByUserId(Long authUserId, BookingPageFilter pageFilter) {
        log.info("Getting all inactive bookings by userId={}", authUserId);

        Pageable pageable = CommonPageable.getPageable(pageFilter, defaultPageNumber, defaultPageSize);
        return bookingRepository.findAllByUserIdAndStatusIn(authUserId, INACTIVE_STATUSES, pageable)
                .map(mapper::toDomain);
    }

    @Cacheable(
            value = "bookingPages",
            key = "@bookingCacheService.registerBookingPageKey(" +
                    "#authUserId, " +
                    "'early:' + @bookingCacheKeys.bookingPage(#authUserId, #pageFilter)" +
                    ")"
    )
    @Transactional(readOnly = true)
    public Page<Booking> getAllEarlyCompletedBookingsByUserId(Long authUserId, BookingPageFilter pageFilter) {
        log.info("Getting all early completed bookings by userId={}", authUserId);

        Pageable pageable = CommonPageable.getPageable(pageFilter, defaultPageNumber, defaultPageSize);
        LocalDate thresholdDate = LocalDate.now().minusMonths(1);
        return bookingRepository.findAllByUserIdAndStatusAndCheckOutDateBefore(
                        authUserId,
                        BookingStatus.CONFIRMED,
                        thresholdDate,
                        pageable
                )
                .map(mapper::toDomain);
    }

    @Cacheable(
            value = "bookingPages",
            key = "@bookingCacheService.registerBookingPageKey(" +
                    "#authUserId, " +
                    "'history:' + @bookingCacheKeys.bookingPage(#authUserId, #pageFilter)" +
                    ")"
    )
    @Transactional(readOnly = true)
    public Page<Booking> getAllHistoryBookingsByUserId(Long authUserId, BookingPageFilter pageFilter) {
        log.info("Getting all booking history by userId={}", authUserId);

        Pageable pageable = CommonPageable.getPageable(pageFilter, defaultPageNumber, defaultPageSize);
        return bookingRepository.findAllByUserId(authUserId, pageable)
                .map(mapper::toDomain);
    }

    public Booking cancelBookingByUuidAndUserId(UUID uuid, Long authUserId) {
        log.info("Cancelling booking: uuid={}, userId={}", uuid, authUserId);

        BookingEntity savedBooking = Optional.ofNullable(transactionTemplate.execute(status -> {
            BookingEntity booking = findBookingEntityByIdAndUserId(uuid, authUserId);

            if (INACTIVE_STATUSES.contains(booking.getStatus())) {
                log.warn("Booking already inactive and can not be cancelled: uuid={}, userId={}, status={}",
                        uuid,
                        authUserId,
                        booking.getStatus()
                );
                throw new IllegalBookingStateException("Booking already inactive id=%s".formatted(uuid));
            }

            switch(booking.getStatus()){
                case BookingStatus.HOLD -> bookingInventoryService.releaseHeldInventory(booking);
                case BookingStatus.CONFIRMED -> bookingInventoryService.releaseConfirmedInventory(booking);
            }

            booking.setStatus(BookingStatus.CANCELLED);
            booking.setHoldExpiresAt(null);
            booking.setCancellationReason("Отменено");

            BookingEntity saved = bookingRepository.save(booking);

            bookingOutboxService.saveEvent(toKafkaEvent(saved, BookingEventType.BOOKING_CANCELLED));

            return saved;
        })).orElseThrow(() -> new IllegalBookingStateException("Booking cancellation transaction returned null result"));

        cacheService.evictBookingByUser(authUserId, uuid);
        cacheService.evictUserPages(authUserId);

        log.info("Booking cancelled successfully: uuid={}, userId={}", uuid, authUserId);
        return mapper.toDomain(savedBooking);
    }

    @Caching(
            put = @CachePut(value = "bookingByUser", key = "@bookingCacheKeys.bookingByUser(#authUserId, #uuid)"),
            evict = {
                    @CacheEvict(value = "bookingPages", allEntries = true),
                    @CacheEvict(value = "bookingSearchPages", allEntries = true)
            }
    )
    @Transactional
    public Booking confirmBookingByUuidAndUserId(UUID uuid, Long authUserId) {
        log.info("Confirming booking: uuid={}, userId={}", uuid, authUserId);
        BookingEntity booking = findBookingEntityByIdAndUserId(uuid, authUserId);

        if (booking.getStatus() != BookingStatus.HOLD) {
            log.warn("Booking must be in HOLD status for confirmation: uuid={}, userId={}, status={}",
                    uuid,
                    authUserId,
                    booking.getStatus()
            );
            throw new IllegalBookingStateException("Booking must be in HOLD status for confirmation id=%s".formatted(uuid));
        }

        if (booking.getHoldExpiresAt() == null || booking.getHoldExpiresAt().isBefore(OffsetDateTime.now())) {
            log.warn("Booking hold already expired: uuid={}, userId={}", uuid, authUserId);
            throw new IllegalBookingStateException("Booking hold already expired id=%s".formatted(uuid));
        }

        bookingInventoryService.confirmHeldInventory(booking);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setHoldExpiresAt(null);
        BookingEntity savedBooking = bookingRepository.save(booking);

        log.info("Sending event to Kafka to confirm booking: eventType={}", BookingEventType.BOOKING_CONFIRMED);
        bookingEventSender.sendEvent(toKafkaEvent(savedBooking, BookingEventType.BOOKING_CONFIRMED));
        log.info("Booking confirmed successfully: uuid={}, userId={}", uuid, authUserId);

        return mapper.toDomain(savedBooking);
    }

    @Transactional(readOnly = true)
    public PageResponse<RoomCategoryResponseDto> searchAvailableRoomCategories(
            AvailableRoomSearchRequestDto request,
           BookingPageFilter pageFilter
    ) {
        log.info("Searching available room categories by dates: checkIn={}, checkOut={}, guests={}",
                request.checkInDate(), request.checkOutDate(), request.guests());

        if (request.checkInDate() == null || request.checkOutDate() == null || !request.checkOutDate().isAfter(request.checkInDate())) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }
        if (request.checkInDate().isBefore(LocalDate.now(bookingValidationProperties.getDateZone()))) {
            throw new IllegalArgumentException("Check-in date can not be in the past");
        }

        var filter = RoomCategorySearchRequestDto.builder()
                .guests(request.guests())
                .roomType(request.roomType())
                .priceFrom(request.priceFrom())
                .priceTo(request.priceTo())
                .minArea(request.minArea())
                .build();

        boolean hasAnyFilter = request.guests() != null || request.roomType() != null
                || request.priceFrom() != null || request.priceTo() != null || request.minArea() != null;

        PageResponse<RoomCategoryResponseDto> rawPage = hasAnyFilter
                ? catalogServiceHttpClient.searchRoomCategories(filter, 0, 100)
                : catalogServiceHttpClient.getRoomCategories(0, 100);

        List<RoomCategoryResponseDto> allRooms = rawPage == null || rawPage.content() == null ? List.of() : rawPage.content();
        List<RoomCategoryResponseDto> availableRooms = allRooms.stream()
                .map(room -> {
                    int availableUnits = bookingInventoryService.getAvailableUnitsForCategory(
                            room.id(),
                            request.checkInDate(),
                            request.checkOutDate(),
                            room.totalUnits()
                    );
                    if (hasAnyFilter && availableUnits <= 0) {
                        return null;
                    }
                    return new RoomCategoryResponseDto(
                            room.id(),
                            room.name(),
                            room.description(),
                            room.maxGuests(),
                            room.basePrice(),
                            room.areaSquare(),
                            room.totalUnits(),
                            availableUnits,
                            room.mainPhotoUrl()
                    );
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        int safePageSize = pageFilter.getPageSize() == null || pageFilter.getPageSize() <= 0
                ? defaultPageSize
                : pageFilter.getPageSize();
        int safePageNumber = pageFilter.getPageNumber() == null
                ? defaultPageNumber
                : Math.max(pageFilter.getPageNumber(), 0);
        int fromIndex = Math.min(safePageNumber * safePageSize, availableRooms.size());
        int toIndex = Math.min(fromIndex + safePageSize, availableRooms.size());
        List<RoomCategoryResponseDto> pageContent = availableRooms.subList(fromIndex, toIndex);
        int totalPages = availableRooms.isEmpty() ? 0 : (int) Math.ceil((double) availableRooms.size() / safePageSize);

        return new PageResponse<>(pageContent, totalPages, availableRooms.size(), safePageSize, safePageNumber);
    }

    @Cacheable(
            value = "bookingSearchPages",
            key = "@bookingCacheService.registerBookingSearchPageKey(" +
                    "#authUserId, " +
                    "@bookingCacheKeys.bookingSearchPage(#authUserId, #bookingFilter, #pageFilter)" +
                    ")"
    )
    @Transactional(readOnly = true)
    public Page<Booking> findAllBookingsByFilterAndByUserId(
            Long authUserId,
            BookingRequestSearchFilter bookingFilter,
            BookingPageFilter pageFilter
    ) {
        log.info("Searching bookings by filter: userId={}, filter={}", authUserId, bookingFilter);
        Pageable pageable = CommonPageable.getPageable(pageFilter, defaultPageNumber, defaultPageSize);
        return bookingRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("userId"), authUserId));

            if (bookingFilter != null) {
                if (bookingFilter.status() != null) {
                    predicates.add(cb.equal(root.get("status"), bookingFilter.status()));
                } else if (bookingFilter.active() != null) {
                    predicates.add(root.get("status").in(bookingFilter.active() ? ACTIVE_STATUSES : INACTIVE_STATUSES));
                }
                if (bookingFilter.adultCount() != null) {
                    predicates.add(cb.equal(root.get("adultCount"), bookingFilter.adultCount()));
                }
                if (bookingFilter.childrenCount() != null) {
                    predicates.add(cb.equal(root.get("childrenCount"), bookingFilter.childrenCount()));
                }
                if (bookingFilter.checkInDate() != null) {
                    predicates.add(cb.equal(root.get("checkInDate"), bookingFilter.checkInDate()));
                }
                if (bookingFilter.checkOutDate() != null) {
                    predicates.add(cb.equal(root.get("checkOutDate"), bookingFilter.checkOutDate()));
                }
            }

            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable).map(mapper::toDomain);
    }

    @Transactional(readOnly = true)
    public boolean existsBookingById(UUID bookingId) {
        return bookingRepository.existsById(bookingId);
    }

    @Transactional(readOnly = true)
    public BookingEntity findBookingEntity(UUID bookingId) {
        log.info("Searching booking entity by id={}", bookingId);
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("Booking entity not found by id={}", bookingId);
                    return new BookingNotFoundException("Booking not found id=%s".formatted(bookingId));
                });
    }

    @Caching(evict = {
            @CacheEvict(value = "bookingByUser", allEntries = true),
            @CacheEvict(value = "bookingPages", allEntries = true),
            @CacheEvict(value = "bookingSearchPages", allEntries = true)
    })
    @Transactional
    public void markBookingFailed(UUID bookingId, String reason) {
        log.warn("Marking booking as failed: bookingId={}, reason={}", bookingId, reason);
        BookingEntity booking = findBookingEntity(bookingId);
        booking.setStatus(BookingStatus.FAILED);
        booking.setCancellationReason(reason);
        booking.setHoldExpiresAt(null);
        BookingEntity savedBooking = bookingRepository.save(booking);

        log.info("Sending event to Kafka to mark booking as failed: eventType={}", BookingEventType.BOOKING_FAILED);
        bookingEventSender.sendEvent(toKafkaEvent(savedBooking, BookingEventType.BOOKING_FAILED));
        log.info("Booking status was updated to {} after starting marking: bookingId={}", booking.getStatus(), bookingId);
    }

    @Caching(evict = {
            @CacheEvict(value = "bookingByUser", allEntries = true),
            @CacheEvict(value = "bookingPages", allEntries = true),
            @CacheEvict(value = "bookingSearchPages", allEntries = true)
    })
    @Transactional
    public void updateBookingPrice(UUID bookingId, BigDecimal priceAmount) {
        log.info("Updating booking price: bookingId={}, priceAmount={}", bookingId, priceAmount);
        BookingEntity booking = findBookingEntity(bookingId);
        booking.setPriceAmount(priceAmount);
        bookingRepository.save(booking);
        log.info("Booking price was updated: bookingId={}, priceAmount={}", bookingId, priceAmount);
    }

    @Caching(evict = {
            @CacheEvict(value = "bookingByUser", allEntries = true),
            @CacheEvict(value = "bookingPages", allEntries = true),
            @CacheEvict(value = "bookingSearchPages", allEntries = true)
    })
    @Transactional
    public void setBookingHold(UUID bookingId, BigDecimal priceAmount, OffsetDateTime holdExpiresAt) {
        log.info("Setting booking hold: bookingId={}, holdExpiresAt={}", bookingId, holdExpiresAt);
        BookingEntity booking = findBookingEntity(bookingId);
        booking.setPriceAmount(priceAmount);
        booking.setStatus(BookingStatus.HOLD);
        booking.setHoldExpiresAt(holdExpiresAt);
        BookingEntity savedBooking = bookingRepository.save(booking);

        log.info("Sending event to Kafka to hold booking: eventType={}", BookingEventType.BOOKING_HOLD_CREATED);
        bookingEventSender.sendEvent(toKafkaEvent(savedBooking, BookingEventType.BOOKING_HOLD_CREATED));
        log.info("Booking status was updated to {} after starting holding: bookingId={}", booking.getStatus(), bookingId);
    }

    @Caching(evict = {
            @CacheEvict(value = "bookingByUser", allEntries = true),
            @CacheEvict(value = "bookingPages", allEntries = true),
            @CacheEvict(value = "bookingSearchPages", allEntries = true)
    })
    @Transactional
    public void expireBooking(UUID bookingId) {
        log.info("Expiring booking: bookingId={}", bookingId);
        BookingEntity booking = findBookingEntity(bookingId);

        if (booking.getStatus() != BookingStatus.HOLD) {
            log.warn("Booking status is not HOLD, expiration skipped: bookingId={}, status={}", bookingId, booking.getStatus());
            return;
        }

        bookingInventoryService.releaseHeldInventory(booking);
        booking.setStatus(BookingStatus.EXPIRED);
        booking.setHoldExpiresAt(null);
        booking.setCancellationReason("Hold expired");
        BookingEntity savedBooking = bookingRepository.save(booking);

        log.info("Sending event to Kafka to expire booking: eventType={}", BookingEventType.BOOKING_EXPIRED);
        bookingEventSender.sendEvent(toKafkaEvent(savedBooking, BookingEventType.BOOKING_EXPIRED));
        log.info("Booking expired successfully: bookingId={}", bookingId);
    }

    @Transactional(readOnly = true)
    public List<BookingEntity> findExpiredHoldBookings() {
        log.debug("Searching for expired hold bookings");
        return bookingRepository.findExpiredHolds(BookingStatus.HOLD, OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<BookingEntity> findExpiredCreatedBookings() {
        log.debug("Searching for expired created bookings");
        return bookingRepository.findExpiredCreatedDrafts(BookingStatus.CREATED, OffsetDateTime.now());
    }

    @Caching(evict = {
            @CacheEvict(value = "bookingByUser", allEntries = true),
            @CacheEvict(value = "bookingPages", allEntries = true),
            @CacheEvict(value = "bookingSearchPages", allEntries = true)
    })
    @Transactional
    public void expireCreatedBooking(UUID bookingId) {
        log.info("Expiring created booking: bookingId={}", bookingId);
        BookingEntity booking = findBookingEntity(bookingId);

        if (booking.getStatus() != BookingStatus.CREATED) {
            log.warn("Booking status is not CREATED, created expiration skipped: bookingId={}, status={}", bookingId, booking.getStatus());
            return;
        }

        booking.setStatus(BookingStatus.EXPIRED);
        booking.setHoldExpiresAt(null);
        booking.setCancellationReason("Booking processing timed out");
        BookingEntity savedBooking = bookingRepository.save(booking);

        log.info("Sending event to Kafka to expire created booking: eventType={}", BookingEventType.BOOKING_EXPIRED);
        bookingEventSender.sendEvent(toKafkaEvent(savedBooking, BookingEventType.BOOKING_EXPIRED));
        log.info("Created booking expired successfully: bookingId={}", bookingId);
    }

    @Transactional(readOnly = true)
    public List<BookingEntity> findHoldBookingsForReminder(OffsetDateTime from, OffsetDateTime to) {
        return bookingRepository.findHoldBookingsForReminder(BookingStatus.HOLD, from, to);
    }

    @Transactional(readOnly = true)
    public List<BookingEntity> findBookingsForCheckInReminder(LocalDate targetDate) {
        return bookingRepository.findBookingsForCheckInReminder(BookingStatus.CONFIRMED, targetDate);
    }

    @Transactional(readOnly = true)
    public List<BookingEntity> findConfirmedBookingsForInventoryRelease(LocalDate today) {
        return bookingRepository.findConfirmedBookingsForInventoryRelease(BookingStatus.CONFIRMED, today);
    }

    @Transactional
    public void releaseInventoryAfterCheckOut(UUID bookingId) {
        log.info("Releasing inventory after check-out: bookingId={}", bookingId);
        BookingEntity booking = findBookingEntity(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED || booking.getInventoryReleasedAt() != null) {
            return;
        }
        bookingInventoryService.releaseConfirmedInventory(booking);
        booking.setInventoryReleasedAt(OffsetDateTime.now());
        String promoCode = promoCodeService.generateForBooking(booking);
        BookingEntity savedBooking = bookingRepository.save(booking);
        sendDirectNotification(
                savedBooking,
                NotificationEventType.BOOKING_REVIEW_REQUEST,
                REVIEW_NOTIFICATION_TITLE,
                buildReviewNotificationMessage(savedBooking, promoCode)
        );
        cacheService.evictUserPages(booking.getUserId());
        cacheService.evictBookingByUser(booking.getUserId(), booking.getId());
    }

    @Caching(evict = {
            @CacheEvict(value = "bookingByUser", allEntries = true),
            @CacheEvict(value = "bookingPages", allEntries = true),
            @CacheEvict(value = "bookingSearchPages", allEntries = true)
    })
    @Transactional
    public void markHoldReminderSent(UUID bookingId, OffsetDateTime sentAt) {
        BookingEntity booking = findBookingEntity(bookingId);
        booking.setHoldReminderSentAt(sentAt);
        bookingRepository.save(booking);
    }

    @Caching(evict = {
            @CacheEvict(value = "bookingByUser", allEntries = true),
            @CacheEvict(value = "bookingPages", allEntries = true),
            @CacheEvict(value = "bookingSearchPages", allEntries = true)
    })
    @Transactional
    public void markCheckInReminderSent(UUID bookingId, OffsetDateTime sentAt) {
        BookingEntity booking = findBookingEntity(bookingId);
        booking.setCheckInReminderSentAt(sentAt);
        bookingRepository.save(booking);
    }

    @Transactional
    public void sendDirectNotification(BookingEntity booking, NotificationEventType type, String title, String message) {
        notificationEventSender.sendEvent(toNotificationKafkaEvent(booking, type, title, message));
    }

    @Caching(evict = {
            @CacheEvict(value = "bookingByUser", allEntries = true),
            @CacheEvict(value = "bookingPages", allEntries = true),
            @CacheEvict(value = "bookingSearchPages", allEntries = true)
    })
    @Transactional
    public void deleteInactiveBookingsCreatedBefore(OffsetDateTime threshold) {
        log.info("Deleting inactive bookings created before {}", threshold);
        int deleted = bookingRepository.deleteInactiveBookingsCreatedBefore(INACTIVE_STATUSES, threshold);
        log.info("Inactive bookings cleanup finished, deletedCount={}", deleted);
    }

    @Caching(evict = {
            @CacheEvict(value = "bookingByUser", allEntries = true),
            @CacheEvict(value = "bookingPages", allEntries = true),
            @CacheEvict(value = "bookingSearchPages", allEntries = true)
    })
    @Transactional
    public int deleteInactiveBookingsByUserId(Long userId) {
        log.info("Deleting inactive bookings for userId={}", userId);
        int deleted = bookingRepository.deleteInactiveBookingsByUserId(userId, INACTIVE_STATUSES);
        log.info("Inactive user bookings cleanup finished: userId={}, deletedCount={}", userId, deleted);
        return deleted;
    }

    @Caching(evict = {
            @CacheEvict(value = "bookingByUser", allEntries = true),
            @CacheEvict(value = "bookingPages", allEntries = true),
            @CacheEvict(value = "bookingSearchPages", allEntries = true)
    })
    @Transactional
    public int deleteCompletedBookingsByUserId(Long userId) {
        log.info("Deleting completed bookings for userId={}", userId);
        int deleted = bookingRepository.deleteCompletedBookingsByUserId(userId, BookingStatus.CONFIRMED, LocalDate.now());
        log.info("Completed user bookings cleanup finished: userId={}, deletedCount={}", userId, deleted);
        return deleted;
    }

    @Caching(evict = {
            @CacheEvict(value = "bookingByUser", allEntries = true),
            @CacheEvict(value = "bookingPages", allEntries = true),
            @CacheEvict(value = "bookingSearchPages", allEntries = true)
    })
    @Transactional
    public void deleteCompletedBookingsCheckedOutBefore(LocalDate thresholdDate) {
        log.info("Deleting completed bookings checked out before {}", thresholdDate);
        int deleted = bookingRepository.deleteCompletedBookingsCheckedOutBefore(BookingStatus.CONFIRMED, thresholdDate);
        log.info("Completed bookings cleanup finished, deletedCount={}", deleted);
    }

    @Transactional(readOnly = true)
    public OffsetDateTime getCleanupThreshold() {
        return OffsetDateTime.now().minus(cleanupRetentionPeriod);
    }

    @Transactional(readOnly = true)
    public LocalDate getCompletedCleanupThresholdDate() {
        return LocalDate.now().minusDays(cleanupRetentionPeriod.toDays());
    }

    private BookingEntity findBookingEntityByIdAndUserId(UUID uuid, Long authUserId) {
        log.info("Searching for booking :bookingId={}, userId={}", uuid, authUserId);
        return bookingRepository.findByIdAndUserId(uuid, authUserId)
                .orElseThrow(() -> {
                    log.warn("Booking not found: uuid={}, userId={}", uuid, authUserId);
                    return new BookingNotFoundException("Booking not found uuid=%s userId=%s".formatted(uuid, authUserId));
                });
    }

    private BookingKafkaEvent<BookingKafkaPayload> toKafkaEvent(BookingEntity booking, BookingEventType type) {
        return BookingKafkaEvent.<BookingKafkaPayload>builder()
                .eventId(UUID.randomUUID())
                .correlationId(booking.getId().toString())
                .source(sourceService)
                .eventType(type)
                .createdAt(OffsetDateTime.now())
                .payload(BookingKafkaPayload.builder()
                        .bookingId(booking.getId())
                        .bookingCode(booking.getBookingCode())
                        .userId(booking.getUserId())
                        .roomCategoryId(booking.getRoomCategoryId())
                        .guests(booking.getGuests())
                        .adultCount(booking.getAdultCount())
                        .childrenCount(booking.getChildrenCount())
                        .checkInDate(booking.getCheckInDate())
                        .checkOutDate(booking.getCheckOutDate())
                        .priceAmount(booking.getPriceAmount())
                        .bookingStatus(booking.getStatus().name())
                        .holdExpiresAt(booking.getHoldExpiresAt())
                        .cancellationReason(booking.getCancellationReason())
                        .build())
                .build();
    }

    private NotificationKafkaEvent<NotificationKafkaPayload> toNotificationKafkaEvent(
            BookingEntity booking,
            NotificationEventType type,
            String title,
            String message
    ) {
        UUID notificationId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        return new NotificationKafkaEvent<>(
                UUID.randomUUID(),
                type,
                sourceService,
                booking.getId().toString(),
                now,
                new NotificationKafkaPayload(
                        notificationId,
                        booking.getUserId(),
                        booking.getId(),
                        null,
                        title,
                        message,
                        type,
                        NotificationStatus.NEW,
                        now
                )
        );
    }

    private String normalizePromoCode(String promoCode) {
        return promoCode == null || promoCode.isBlank() ? null : promoCode.trim().toUpperCase();
    }

    private String buildReviewNotificationMessage(BookingEntity booking, String promoCode) {
        StringBuilder builder = new StringBuilder()
                .append("Спасибо, что выбрали River Park. Все понравилось? Оставьте отзыв на сайте отеля.");
        if (promoCode != null && !promoCode.isBlank()) {
            builder.append("\n\nВаш промокод на следующее заселение: ")
                    .append(promoCode);
            if (booking.getPromoDiscountPercent() != null) {
                builder.append(" (-").append(booking.getPromoDiscountPercent()).append("%)");
            }
        }
        return builder.toString();
    }
}
