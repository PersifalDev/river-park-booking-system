package ru.haritonenko.bookingservice.external.client.users;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import ru.haritonenko.commonlibs.dto.users.UserResponseDto;

@HttpExchange(accept = "application/json", contentType = "application/json")
public interface UserServiceHttpClient {

    @GetExchange("/users/{id}")
    UserResponseDto getUserById(@PathVariable Long id);
}
