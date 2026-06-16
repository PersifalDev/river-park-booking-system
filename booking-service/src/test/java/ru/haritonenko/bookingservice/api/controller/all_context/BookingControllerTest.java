package ru.haritonenko.bookingservice.api.controller.all_context;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.haritonenko.bookingservice.AbstractIntegrationTest;
import ru.haritonenko.bookingservice.api.dto.BookingRequestDto;
import ru.haritonenko.bookingservice.api.dto.BookingResponseDto;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingEntityRepository;
import ru.haritonenko.bookingservice.domain.service.BookingCodeGenerator;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import ru.haritonenko.bookingservice.external.client.catalog.CatalogServiceHttpClient;
import ru.haritonenko.bookingservice.security.jwt.manager.JwtTokenManager;
import ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.AsyncBookingTaskDispatcher;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "jwt.secret-key=RPK_jwt_2026_8fC2mQ7pL1xN9vZ4aT6yH3kR0sD5wB8uE1cJ7nM4qX2pV9rL6gS0tY3uF5iK8oA1",
        "jwt.lifetime=86400000"
})
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

    @MockBean
    private CatalogServiceHttpClient catalogServiceHttpClient;

    @MockBean
    private AsyncBookingTaskDispatcher asyncBookingTaskDispatcher;

    private String jwt;

    private AuthUser authUser;

    @BeforeEach
    void setUp() {
        bookingEntityRepository.deleteAll();

        jwt = jwtTokenManager.generateToken(
                1L,
                "test-user",
                "USER"
        );

        authUser = AuthUser.builder()
                .id(1L)
                .login("test-user")
                .role("USER")
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
        Assertions.assertEquals(bookingEntity.getHoldExpiresAt().toInstant(), bookingResponseDto.holdExpiresAt().toInstant());
        Assertions.assertEquals(bookingEntity.getHasPromo(), bookingResponseDto.hasPromo());
        Assertions.assertEquals(bookingEntity.getStatus(), bookingResponseDto.status());
        Assertions.assertEquals(bookingEntity.getCancellationReason(), bookingResponseDto.cancellationReason());

    }

    private BookingRequestDto createDummyBookingRequest() {

        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = checkInDate.plusDays(2);

        return BookingRequestDto.builder()
                .categoryId(1L)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .guests(2)
                .adultCount(2)
                .childrenCount(0)
                .promoCode(null)
                .build();
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
                .holdExpiresAt(now.plus(holdTtl))
                .hasPromo(bookingRequestDto.promoCode() != null && !bookingRequestDto.promoCode().isBlank())
                .status(BookingStatus.CREATED)
                .build());
    }

}
