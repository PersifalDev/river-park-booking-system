package ru.haritonenko.bookingservice.config.page;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.booking.page")
public class BookingPageProperties {

    private int defaultNumber;
    private int defaultSize;
    private int catalogSearchNumber;
    private int catalogSearchSize;
    private int earlyCompletedMonthsBefore;
}
