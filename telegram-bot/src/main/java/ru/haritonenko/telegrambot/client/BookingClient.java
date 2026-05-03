package ru.haritonenko.telegrambot.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.utils.pages.PageResponse;
import ru.haritonenko.telegrambot.dto.booking.BotAvailableRoomSearchRequestDto;
import ru.haritonenko.telegrambot.dto.booking.BotBookingRequestDto;
import ru.haritonenko.telegrambot.dto.booking.BotBookingResponseDto;
import ru.haritonenko.telegrambot.dto.common.BotPageResponse;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookingClient {

    private static final ParameterizedTypeReference<BotPageResponse<BotBookingResponseDto>> BOOKING_PAGE_TYPE =
            new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<PageResponse<RoomCategoryResponseDto>> ROOM_PAGE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient bookingRestClient;

    public BotBookingResponseDto createBooking(String jwt, BotBookingRequestDto request) {
        return bookingRestClient.post()
                .uri("/booking")
                .header(HttpHeaders.AUTHORIZATION, bearer(jwt))
                .body(request)
                .retrieve()
                .body(BotBookingResponseDto.class);
    }

    public BotBookingResponseDto getBooking(String jwt, UUID bookingId) {
        return bookingRestClient.get()
                .uri("/booking/{id}", bookingId)
                .header(HttpHeaders.AUTHORIZATION, bearer(jwt))
                .retrieve()
                .body(BotBookingResponseDto.class);
    }

    public List<BotBookingResponseDto> getBookings(String jwt, int pageNumber, int pageSize) {
        BotPageResponse<BotBookingResponseDto> response = bookingRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/booking")
                        .queryParam("pageNumber", pageNumber)
                        .queryParam("pageSize", pageSize)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, bearer(jwt))
                .retrieve()
                .body(BOOKING_PAGE_TYPE);

        return response == null || response.content() == null
                ? List.of()
                : response.content();
    }

    public PageResponse<RoomCategoryResponseDto> searchAvailableRooms(
            String jwt,
            BotAvailableRoomSearchRequestDto request,
            int pageNumber,
            int pageSize
    ) {
        var response = bookingRestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/booking/available/search")
                        .queryParam("pageNumber", pageNumber)
                        .queryParam("pageSize", pageSize)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, bearer(jwt))
                .body(request)
                .retrieve()
                .body(ROOM_PAGE_TYPE);
        return response == null ? new PageResponse<>(List.of(), 0, 0, pageSize, pageNumber) : response;
    }

    public BotBookingResponseDto cancelBooking(String jwt, UUID bookingId) {
        return bookingRestClient.patch()
                .uri("/booking/{id}/cancel", bookingId)
                .header(HttpHeaders.AUTHORIZATION, bearer(jwt))
                .retrieve()
                .body(BotBookingResponseDto.class);
    }

    public BotBookingResponseDto confirmBooking(String jwt, UUID bookingId) {
        return bookingRestClient.patch()
                .uri("/booking/{id}/confirm", bookingId)
                .header(HttpHeaders.AUTHORIZATION, bearer(jwt))
                .retrieve()
                .body(BotBookingResponseDto.class);
    }

    private String bearer(String jwt) {
        return "Bearer " + jwt;
    }
}
