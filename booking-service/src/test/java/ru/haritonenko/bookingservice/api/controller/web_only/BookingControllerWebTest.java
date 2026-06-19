package ru.haritonenko.bookingservice.api.controller.web_only;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.haritonenko.bookingservice.api.controller.BookingController;
import ru.haritonenko.bookingservice.api.dto.AvailableRoomSearchRequestDto;
import ru.haritonenko.bookingservice.api.dto.BookingRequestDto;
import ru.haritonenko.bookingservice.api.dto.BookingResponseDto;
import ru.haritonenko.bookingservice.api.dto.TariffResponseDto;
import ru.haritonenko.bookingservice.api.dto.filter.BookingPageFilter;
import ru.haritonenko.bookingservice.api.dto.filter.BookingRequestSearchFilter;
import ru.haritonenko.bookingservice.config.validation.BookingValidationProperties;
import ru.haritonenko.bookingservice.domain.Booking;
import ru.haritonenko.bookingservice.domain.mapper.BookingToResponseDtoMapper;
import ru.haritonenko.bookingservice.domain.service.BookingService;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import ru.haritonenko.bookingservice.domain.tariff.TariffCancellationPolicy;
import ru.haritonenko.bookingservice.domain.tariff.TariffPriceModifierType;
import ru.haritonenko.bookingservice.security.service.AuthenticationService;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;
import ru.haritonenko.commonlibs.utils.pages.PageResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookingController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@EnableConfigurationProperties(BookingValidationProperties.class)
class BookingControllerWebTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private BookingToResponseDtoMapper mapper;

    private Booking booking;

    private BookingResponseDto response;

    @BeforeEach
    void setUp() {
        booking = booking(BookingStatus.HOLD);
        response = response(booking);

        when(authenticationService.getCurrentAuthenticatedUser())
                .thenReturn(AuthUser.builder()
                        .id(USER_ID)
                        .login("test-user")
                        .role("USER")
                        .build());
        when(mapper.toDto(any(Booking.class))).thenAnswer(invocation -> response(invocation.getArgument(0)));
    }

    @Test
    void shouldCreateBooking() throws Exception {
        BookingRequestDto request = bookingRequest();
        when(bookingService.createBooking(any(BookingRequestDto.class), eq(USER_ID), eq("idem-key")))
                .thenReturn(booking);

        mockMvc.perform(post("/booking")
                        .header("Idempotency-Key", "idem-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(booking.id().toString()))
                .andExpect(jsonPath("$.status").value(BookingStatus.HOLD.getValue()));

        verify(bookingService).createBooking(any(BookingRequestDto.class), eq(USER_ID), eq("idem-key"));
    }

    @Test
    void shouldSearchAvailableRooms() throws Exception {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = checkInDate.plusDays(1);
        AvailableRoomSearchRequestDto request = new AvailableRoomSearchRequestDto(
                checkInDate,
                checkOutDate,
                2,
                RoomType.STANDARD,
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(7000),
                BigDecimal.valueOf(15)
        );
        when(bookingService.searchAvailableRoomCategories(any(AvailableRoomSearchRequestDto.class), any(BookingPageFilter.class)))
                .thenReturn(new PageResponse<>(
                        List.of(room()),
                        1,
                        1,
                        10,
                        0
                ));

        mockMvc.perform(post("/booking/available/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .param("pageNumber", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1));

        verify(bookingService).searchAvailableRoomCategories(any(AvailableRoomSearchRequestDto.class), any(BookingPageFilter.class));
    }

    @Test
    void shouldSearchBookings() throws Exception {
        when(bookingService.findAllBookingsByFilterAndByUserId(eq(USER_ID), any(BookingRequestSearchFilter.class), any(BookingPageFilter.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));

        mockMvc.perform(post("/booking/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "HOLD",
                                  "adultCount": 2
                                }
                                """)
                        .param("pageNumber", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value(BookingStatus.HOLD.getValue()));

        verify(bookingService).findAllBookingsByFilterAndByUserId(eq(USER_ID), any(BookingRequestSearchFilter.class), any(BookingPageFilter.class));
    }

    @Test
    void shouldGetAvailableTariffs() throws Exception {
        BookingRequestDto request = bookingRequest();
        when(bookingService.findApplicableTariffs(any(BookingRequestDto.class)))
                .thenReturn(List.of(new TariffResponseDto(
                        "BREAKFAST",
                        "С завтраком",
                        "Тариф с завтраком",
                        BigDecimal.valueOf(5500),
                        TariffPriceModifierType.FIXED_PER_NIGHT,
                        BigDecimal.valueOf(500),
                        TariffCancellationPolicy.FREE_UNTIL_DEADLINE,
                        2,
                        "Проживание; завтрак",
                        1,
                        null,
                        1,
                        null
                )));

        mockMvc.perform(post("/booking/tariffs/available")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("BREAKFAST"))
                .andExpect(jsonPath("$[0].title").value("С завтраком"))
                .andExpect(jsonPath("$[0].priceAmount").value(5500))
                .andExpect(jsonPath("$[0].cancellationPolicy").value("FREE_UNTIL_DEADLINE"));

        verify(bookingService).findApplicableTariffs(any(BookingRequestDto.class));
    }

    @Test
    void shouldGetBookingByUuid() throws Exception {
        when(bookingService.getBookingByUuidAndUserId(USER_ID, booking.id())).thenReturn(booking);

        mockMvc.perform(get("/booking/{uuid}", booking.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(booking.id().toString()));

        verify(bookingService).getBookingByUuidAndUserId(USER_ID, booking.id());
    }

    @Test
    void shouldGetActiveBookings() throws Exception {
        when(bookingService.getAllActiveBookingsByUserId(eq(USER_ID), any(BookingPageFilter.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));

        mockMvc.perform(get("/booking")
                        .param("pageNumber", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(bookingService).getAllActiveBookingsByUserId(eq(USER_ID), any(BookingPageFilter.class));
    }

    @Test
    void shouldGetInactiveBookings() throws Exception {
        Booking inactiveBooking = booking(BookingStatus.CANCELLED);
        when(bookingService.getAllInactiveBookingsByUserId(eq(USER_ID), any(BookingPageFilter.class)))
                .thenReturn(new PageImpl<>(List.of(inactiveBooking)));

        mockMvc.perform(get("/booking/inactive")
                        .param("pageNumber", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value(BookingStatus.CANCELLED.getValue()));

        verify(bookingService).getAllInactiveBookingsByUserId(eq(USER_ID), any(BookingPageFilter.class));
    }

    @Test
    void shouldGetEarlyBookings() throws Exception {
        when(bookingService.getAllEarlyCompletedBookingsByUserId(eq(USER_ID), any(BookingPageFilter.class)))
                .thenReturn(new PageImpl<>(List.of(booking(BookingStatus.CONFIRMED))));

        mockMvc.perform(get("/booking/early")
                        .param("pageNumber", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(bookingService).getAllEarlyCompletedBookingsByUserId(eq(USER_ID), any(BookingPageFilter.class));
    }

    @Test
    void shouldGetHistoryBookings() throws Exception {
        when(bookingService.getAllHistoryBookingsByUserId(eq(USER_ID), any(BookingPageFilter.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));

        mockMvc.perform(get("/booking/history")
                        .param("pageNumber", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(bookingService).getAllHistoryBookingsByUserId(eq(USER_ID), any(BookingPageFilter.class));
    }

    @Test
    void shouldClearInactiveBookings() throws Exception {
        mockMvc.perform(delete("/booking/inactive"))
                .andExpect(status().isNoContent());

        verify(bookingService).deleteInactiveBookingsByUserId(USER_ID);
    }

    @Test
    void shouldClearCompletedBookings() throws Exception {
        mockMvc.perform(delete("/booking/completed"))
                .andExpect(status().isNoContent());

        verify(bookingService).deleteCompletedBookingsByUserId(USER_ID);
    }

    @Test
    void shouldCancelBooking() throws Exception {
        Booking cancelledBooking = booking(BookingStatus.CANCELLED);
        when(bookingService.cancelBookingByUuidAndUserId(booking.id(), USER_ID)).thenReturn(cancelledBooking);

        mockMvc.perform(patch("/booking/{uuid}/cancel", booking.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BookingStatus.CANCELLED.getValue()));

        verify(bookingService).cancelBookingByUuidAndUserId(booking.id(), USER_ID);
    }

    @Test
    void shouldConfirmBooking() throws Exception {
        Booking confirmedBooking = booking(BookingStatus.CONFIRMED);
        when(bookingService.confirmBookingByUuidAndUserId(booking.id(), USER_ID)).thenReturn(confirmedBooking);

        mockMvc.perform(patch("/booking/{uuid}/confirm", booking.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BookingStatus.CONFIRMED.getValue()));

        verify(bookingService).confirmBookingByUuidAndUserId(booking.id(), USER_ID);
    }

    @Test
    void shouldRejectInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private BookingRequestDto bookingRequest() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        return BookingRequestDto.builder()
                .categoryId(1L)
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(1))
                .guests(2)
                .adultCount(2)
                .childrenCount(0)
                .build();
    }

    private Booking booking(BookingStatus status) {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        return Booking.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .roomCategoryId(1L)
                .bookingCode("BK-TEST")
                .guests(2)
                .adultCount(2)
                .childrenCount(0)
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(1))
                .priceAmount(BigDecimal.valueOf(5000))
                .tariffCode("ROOM_ONLY")
                .tariffTitle("Без завтрака")
                .tariffCancellationPolicy("FLEXIBLE")
                .tariffFreeCancellationDaysBefore(1)
                .tariffIncludedServices("Проживание")
                .holdExpiresAt(status == BookingStatus.HOLD ? OffsetDateTime.now().plusMinutes(15) : null)
                .hasPromo(false)
                .status(status)
                .cancellationReason(status == BookingStatus.CANCELLED ? "Отменено" : null)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private BookingResponseDto response(Booking booking) {
        return new BookingResponseDto(
                booking.id(),
                booking.bookingCode(),
                booking.userId(),
                booking.roomCategoryId(),
                booking.roomNumberSnapshot(),
                booking.guests(),
                booking.adultCount(),
                booking.childrenCount(),
                booking.checkInDate(),
                booking.checkOutDate(),
                booking.priceAmount(),
                booking.tariffCode(),
                booking.tariffTitle(),
                booking.tariffCancellationPolicy(),
                booking.tariffFreeCancellationDaysBefore(),
                booking.tariffIncludedServices(),
                booking.holdExpiresAt(),
                booking.hasPromo(),
                booking.appliedPromoCode(),
                booking.generatedPromoCode(),
                booking.promoDiscountPercent(),
                booking.status(),
                booking.cancellationReason(),
                booking.createdAt(),
                booking.updatedAt()
        );
    }

    private RoomCategoryResponseDto room() {
        return new RoomCategoryResponseDto(
                1L,
                RoomType.STANDARD,
                "Standard room",
                2,
                BigDecimal.valueOf(5000),
                20.0,
                10,
                7,
                null
        );
    }
}
