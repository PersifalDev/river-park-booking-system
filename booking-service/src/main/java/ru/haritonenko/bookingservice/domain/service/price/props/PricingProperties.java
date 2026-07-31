package ru.haritonenko.bookingservice.domain.service.price.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.booking.pricing")
public record PricingProperties(
    BigDecimal defaultPrice
){}

