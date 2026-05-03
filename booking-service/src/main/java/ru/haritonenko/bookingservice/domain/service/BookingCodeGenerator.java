package ru.haritonenko.bookingservice.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@Slf4j
public class BookingCodeGenerator {

    public String generate() {
        log.info("Generating booking code");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        String code = "BK-" + timestamp + "-" + suffix;
        log.info("Code={} was generated",code);
        return code;
    }
}
