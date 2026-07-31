package ru.haritonenko.bookingservice.api.controller.all_context;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.haritonenko.bookingservice.domain.service.AbstractIntegrationTest;
import ru.haritonenko.bookingservice.api.dto.BookingRequestDto;
import ru.haritonenko.bookingservice.api.dto.BookingResponseDto;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingEntityRepository;
import ru.haritonenko.bookingservice.domain.service.BookingCodeGenerator;
import ru.haritonenko.bookingservice.domain.service.BookingInventoryService;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import ru.haritonenko.bookingservice.external.client.catalog.CatalogServiceHttpClient;
import ru.haritonenko.bookingservice.kafka.outbox.service.BookingOutboxService;
import ru.haritonenko.bookingservice.kafka.producer.booking.sender.KafkaBookingEventSender;
import ru.haritonenko.bookingservice.security.jwt.manager.JwtTokenManager;
import ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.AsyncBookingTaskDispatcher;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;
import ru.haritonenko.commonlibs.utils.pages.PageResponse;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class BookingControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenManager jwtTokenManager;

    @Autowired
    private BookingEntityRepository bookingEntityRepository;

    @Autowired
    private BookingCodeGenerator bookingCodeGenerator;

    @MockitoBean
    private CatalogServiceHttpClient catalogServiceHttpClient;

    @MockitoBean
    private AsyncBookingTaskDispatcher asyncBookingTaskDispatcher;

    @MockitoBean
    private BookingInventoryService bookingInventoryService;

    @MockitoBean
    private KafkaBookingEventSender bookingEventSender;

    @MockitoBean
    private BookingOutboxService bookingOutboxService;

    private String jwt;

    private AuthUser authUser;

    @BeforeEach
    void setUp() {
        bookingEntityRepository.deleteAll();

        jwt = jwtTokenManager.generateToken(
                1L,
                "test-user",
                "BOOKING_MANAGER"
        );

        authUser = AuthUser.builder()
                .id(1L)
                .login("test-user")
                .role("BOOKING_MANAGER")
                .build();


        when(catalogServiceHttpClient.getRoomCategoryById(1L))
                .thenReturn(new RoomCategoryResponseDto(
                        1L,
                        RoomType.STANDARD,
                        "Standard room",
                        2,
                        BigDecimal.valueOf(5000),
                        20.0,
                        10,
                        null,
                        null
                ));


        doNothing().when(asyncBookingTaskDispatcher).dispatchTask(any());
        doNothing().when(bookingInventoryService).releaseHeldInventory(any());
        doNothing().when(bookingInventoryService).releaseConfirmedInventory(any());
        doNothing().when(bookingInventoryService).confirmHeldInventory(any());
        when(bookingEventSender.sendEvent(any())).thenReturn(CompletableFuture.completedFuture(null));
        doNothing().when(bookingOutboxService).saveEvent(any());
    }

    @Test
    @SneakyThrows
    void shouldSuccessfullyCreateBooking() {

        var booking = createDummyBookingRequest();

        String bookingJson = objectMapper.writeValueAsString(booking);

        String createdBookingJson = mockMvc.perform(post("/booking")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson)
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        BookingResponseDto bookingResponseDto =
                objectMapper.readValue(createdBookingJson, BookingResponseDto.class);

        Assertions.assertEquals(authUser.id(), bookingResponseDto.userId());
        Assertions.assertEquals(booking.categoryId(), bookingResponseDto.roomCategoryId());
        Assertions.assertEquals(booking.checkInDate(), bookingResponseDto.checkInDate());
        Assertions.assertEquals(booking.checkOutDate(), bookingResponseDto.checkOutDate());
        Assertions.assertEquals(booking.guests(), bookingResponseDto.guests());
        Assertions.assertEquals(booking.adultCount(), bookingResponseDto.adultCount());
        Assertions.assertEquals(booking.childrenCount(), bookingResponseDto.childrenCount());
        Assertions.assertEquals(BookingStatus.CREATED, bookingResponseDto.status());

        Assertions.assertNotNull(bookingResponseDto.bookingCode());
        Assertions.assertNotNull(bookingResponseDto.userId());
        Assertions.assertNotNull(bookingResponseDto.priceAmount());
        Assertions.assertNotNull(bookingResponseDto.holdExpiresAt());
        Assertions.assertNotNull(bookingResponseDto.holdExpiresAt());

        verify(asyncBookingTaskDispatcher).dispatchTask(any());

    }

    @Test
    @SneakyThrows
    void shouldReturnActiveBookings() {
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.HOLD);
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.CONFIRMED);
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.CANCELLED);

        mockMvc.perform(get("/booking")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .param("pageNumber", "0")
                        .param("pageSize", "10")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].status").value(BookingStatus.HOLD.getValue()))
                .andExpect(jsonPath("$.content[1].status").value(BookingStatus.CONFIRMED.getValue()));
    }

    @Test
    @SneakyThrows
    void shouldReturnInactiveBookings() {
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.CANCELLED);
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.EXPIRED);
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.FAILED);
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.HOLD);

        mockMvc.perform(get("/booking/inactive")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .param("pageNumber", "0")
                        .param("pageSize", "10")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    @SneakyThrows
    void shouldReturnEarlyCompletedBookings() {
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.CONFIRMED, LocalDate.now().minusMonths(2));
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.CONFIRMED, LocalDate.now().minusDays(2));
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.CANCELLED, LocalDate.now().minusMonths(2));

        mockMvc.perform(get("/booking/early")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .param("pageNumber", "0")
                        .param("pageSize", "10")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value(BookingStatus.CONFIRMED.getValue()));
    }

    @Test
    @SneakyThrows
    void shouldReturnBookingHistory() {
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.HOLD);
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.CANCELLED);
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.CONFIRMED);

        mockMvc.perform(get("/booking/history")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .param("pageNumber", "0")
                        .param("pageSize", "10")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    @SneakyThrows
    void shouldSearchBookingsByFilter() {
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.HOLD);
        createDummyBookingEntity(createDummyBookingRequest(3, 2, 1), BookingStatus.HOLD);
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.CANCELLED);

        String filterJson = """
                {
                  "status": "HOLD",
                  "adultCount": 2,
                  "childrenCount": 0
                }
                """;

        mockMvc.perform(post("/booking/search")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterJson)
                        .param("pageNumber", "0")
                        .param("pageSize", "10")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value(BookingStatus.HOLD.getValue()))
                .andExpect(jsonPath("$.content[0].childrenCount").value(0));
    }

    @Test
    @SneakyThrows
    void shouldSearchAvailableRooms() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = checkInDate.plusDays(1);

        when(catalogServiceHttpClient.searchRoomCategories(any(), eq(0), eq(100)))
                .thenReturn(new PageResponse<>(
                        List.of(new RoomCategoryResponseDto(
                                1L,
                                RoomType.STANDARD,
                                "Standard room",
                                2,
                                BigDecimal.valueOf(5000),
                                20.0,
                                10,
                                null,
                                null
                        )),
                        1,
                        1,
                        100,
                        0
                ));
        when(bookingInventoryService.getAvailableUnitsForCategory(1L, checkInDate, checkOutDate, 10))
                .thenReturn(7);

        String requestJson = """
                {
                  "checkInDate": "%s",
                  "checkOutDate": "%s",
                  "guests": 2,
                  "roomType": "STANDARD",
                  "priceFrom": 4000,
                  "priceTo": 7000,
                  "minArea": 15
                }
                """.formatted(checkInDate, checkOutDate);

        mockMvc.perform(post("/booking/available/search")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .param("pageNumber", "0")
                        .param("pageSize", "10")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].availableUnits").value(7));
    }

    @Test
    @SneakyThrows
    void shouldCancelBooking() {
        BookingEntity booking = createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.HOLD);

        String cancelledBookingJson = mockMvc.perform(patch("/booking/{uuid}/cancel", booking.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BookingStatus.CANCELLED.getValue()))
                .andExpect(jsonPath("$.cancellationReason").value("Отменено"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        BookingResponseDto bookingResponseDto = objectMapper.readValue(cancelledBookingJson, BookingResponseDto.class);
        Assertions.assertNull(bookingResponseDto.holdExpiresAt());
        verify(bookingInventoryService).releaseHeldInventory(any());
        verify(bookingOutboxService).saveEvent(any());
    }

    @Test
    @SneakyThrows
    void shouldConfirmBooking() {
        BookingEntity booking = createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.HOLD);

        mockMvc.perform(patch("/booking/{uuid}/confirm", booking.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BookingStatus.CONFIRMED.getValue()))
                .andExpect(jsonPath("$.holdExpiresAt").doesNotExist());

        verify(bookingInventoryService).confirmHeldInventory(any());
        verify(bookingOutboxService).saveEvent(any());
    }

    @Test
    @SneakyThrows
    void shouldClearInactiveBookings() {
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.CANCELLED);
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.EXPIRED);
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.HOLD);

        mockMvc.perform(delete("/booking/inactive")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                )
                .andDo(print())
                .andExpect(status().isNoContent());

        Assertions.assertEquals(1, bookingEntityRepository.count());
        Assertions.assertEquals(BookingStatus.HOLD, bookingEntityRepository.findAll().getFirst().getStatus());
    }

    @Test
    @SneakyThrows
    void shouldClearCompletedBookings() {
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.CONFIRMED, LocalDate.now().minusDays(1));
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.CONFIRMED, LocalDate.now().plusDays(1));
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.HOLD);

        mockMvc.perform(delete("/booking/completed")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                )
                .andDo(print())
                .andExpect(status().isNoContent());

        Assertions.assertEquals(2, bookingEntityRepository.count());
    }

    @Test
    @SneakyThrows
    void shouldRejectUnauthorizedRequest() {
        mockMvc.perform(get("/booking"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Failed to authenticate"));
    }

    @Test
    @SneakyThrows
    void shouldRejectRequestWithInvalidBearerToken() {
        mockMvc.perform(get("/booking")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .param("pageNumber", "0")
                        .param("pageSize", "10")
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Failed to authenticate"));
    }

    @Test
    @SneakyThrows
    void shouldReturnOnlyCurrentUserBookings() {
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.HOLD);
        createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.HOLD, 2L);

        mockMvc.perform(get("/booking")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .param("pageNumber", "0")
                        .param("pageSize", "10")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(authUser.id()));
    }

    @Test
    @SneakyThrows
    void shouldNotReturnBookingOwnedByAnotherUser() {
        BookingEntity otherUserBooking = createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.HOLD, 2L);

        mockMvc.perform(get("/booking/{uuid}", otherUserBooking.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detailedMessage").value("BookingNotFoundException"));
    }

    @Test
    @SneakyThrows
    void shouldNotCancelBookingOwnedByAnotherUser() {
        BookingEntity otherUserBooking = createDummyBookingEntity(createDummyBookingRequest(), BookingStatus.HOLD, 2L);

        mockMvc.perform(patch("/booking/{uuid}/cancel", otherUserBooking.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detailedMessage").value("BookingNotFoundException"));

        BookingEntity unchangedBooking = bookingEntityRepository.findById(otherUserBooking.getId()).orElseThrow();
        Assertions.assertEquals(BookingStatus.HOLD, unchangedBooking.getStatus());
        verify(bookingInventoryService, never()).releaseHeldInventory(any());
    }

    @Test
    @SneakyThrows
    void shouldSuccessfullyReturnBookingByUuid() {

        var booking = createDummyBookingRequest();

        var bookingEntity = createDummyBookingEntity(
                booking,
                OffsetDateTime.now(),
                Duration.ofMinutes(15)
        );
        String foundBookingJson = mockMvc.perform(get("/booking/{uuid}", bookingEntity.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        BookingResponseDto bookingResponseDto =
                objectMapper.readValue(foundBookingJson, BookingResponseDto.class);

        Assertions.assertNotNull(bookingResponseDto.id());
        Assertions.assertEquals(bookingEntity.getBookingCode(), bookingResponseDto.bookingCode());
        Assertions.assertEquals(bookingEntity.getUserId(), bookingResponseDto.userId());
        Assertions.assertEquals(bookingEntity.getRoomCategoryId(), bookingResponseDto.roomCategoryId());
        Assertions.assertEquals(bookingEntity.getGuests(), bookingResponseDto.guests());
        Assertions.assertEquals(bookingEntity.getAdultCount(), bookingResponseDto.adultCount());
        Assertions.assertEquals(bookingEntity.getChildrenCount(), bookingResponseDto.childrenCount());
        Assertions.assertEquals(bookingEntity.getCheckInDate(), bookingResponseDto.checkInDate());
        Assertions.assertEquals(bookingEntity.getCheckOutDate(), bookingResponseDto.checkOutDate());
        Assertions.assertEquals(
                0,
                bookingEntity.getPriceAmount().compareTo(bookingResponseDto.priceAmount())
        );
        assertInstantCloseTo(bookingEntity.getHoldExpiresAt().toInstant(), bookingResponseDto.holdExpiresAt().toInstant());
        Assertions.assertEquals(bookingEntity.getHasPromo(), bookingResponseDto.hasPromo());
        Assertions.assertEquals(bookingEntity.getStatus(), bookingResponseDto.status());
        Assertions.assertEquals(bookingEntity.getCancellationReason(), bookingResponseDto.cancellationReason());

    }

    private void assertInstantCloseTo(Instant expected, Instant actual) {
        long differenceNanos = Math.abs(Duration.between(expected, actual).toNanos());
        Assertions.assertTrue(
                differenceNanos < Duration.ofMillis(1).toNanos(),
                "Expected <%s> to be within 1 ms of <%s>".formatted(actual, expected)
        );
    }

    private BookingRequestDto createDummyBookingRequest() {
        return createDummyBookingRequest(2, 2, 0);
    }

    private BookingRequestDto createDummyBookingRequest(int guests, int adultCount, int childrenCount) {

        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = checkInDate.plusDays(2);

        return BookingRequestDto.builder()
                .categoryId(1L)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .guests(guests)
                .adultCount(adultCount)
                .childrenCount(childrenCount)
                .promoCode(null)
                .build();
    }

    private String bearer() {
        return "Bearer " + jwt;
    }

    private BookingEntity createDummyBookingEntity(
            BookingRequestDto bookingRequestDto,
            BookingStatus status
    ) {
        return createDummyBookingEntity(bookingRequestDto, status, bookingRequestDto.checkOutDate());
    }

    private BookingEntity createDummyBookingEntity(
            BookingRequestDto bookingRequestDto,
            BookingStatus status,
            LocalDate checkOutDate
    ) {
        return createDummyBookingEntity(bookingRequestDto, status, checkOutDate, authUser.id());
    }

    private BookingEntity createDummyBookingEntity(
            BookingRequestDto bookingRequestDto,
            BookingStatus status,
            Long userId
    ) {
        return createDummyBookingEntity(bookingRequestDto, status, bookingRequestDto.checkOutDate(), userId);
    }

    private BookingEntity createDummyBookingEntity(
            BookingRequestDto bookingRequestDto,
            BookingStatus status,
            LocalDate checkOutDate,
            Long userId
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        return bookingEntityRepository.save(BookingEntity.builder()
                .userId(userId)
                .roomCategoryId(bookingRequestDto.categoryId())
                .bookingCode(bookingCodeGenerator.generate())
                .guests(bookingRequestDto.guests())
                .adultCount(bookingRequestDto.adultCount())
                .childrenCount(bookingRequestDto.childrenCount())
                .checkInDate(checkOutDate.minusDays(1))
                .checkOutDate(checkOutDate)
                .priceAmount(BigDecimal.ONE)
                .tariffCode("ROOM_ONLY")
                .tariffTitle("Room only")
                .tariffCancellationPolicy("FLEXIBLE")
                .tariffFreeCancellationDaysBefore(1)
                .tariffIncludedServices("Accommodation")
                .holdExpiresAt(status == BookingStatus.HOLD ? now.plus(Duration.ofMinutes(15)) : null)
                .hasPromo(bookingRequestDto.promoCode() != null && !bookingRequestDto.promoCode().isBlank())
                .status(status)
                .cancellationReason(status == BookingStatus.CANCELLED ? "Отменено" : null)
                .build());
    }

    private BookingEntity createDummyBookingEntity(
            BookingRequestDto bookingRequestDto,
            OffsetDateTime now,
            Duration holdTtl
    ){
        return bookingEntityRepository.save(BookingEntity.builder()
                .userId(authUser.id())
                .roomCategoryId(bookingRequestDto.categoryId())
                .bookingCode(bookingCodeGenerator.generate())
                .guests(bookingRequestDto.guests())
                .adultCount(bookingRequestDto.adultCount())
                .childrenCount(bookingRequestDto.childrenCount())
                .checkInDate(bookingRequestDto.checkInDate())
                .checkOutDate(bookingRequestDto.checkOutDate())
                .priceAmount(BigDecimal.ONE)
                .tariffCode("ROOM_ONLY")
                .tariffTitle("Room only")
                .tariffCancellationPolicy("FLEXIBLE")
                .tariffFreeCancellationDaysBefore(1)
                .tariffIncludedServices("Accommodation")
                .holdExpiresAt(now.plus(holdTtl))
                .hasPromo(bookingRequestDto.promoCode() != null && !bookingRequestDto.promoCode().isBlank())
                .status(BookingStatus.CREATED)
                .build());
    }

}
