package ru.haritonenko.bookingservice.config.workmode;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.haritonenko.commonlibs.communication.WorkMode;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.booking")
public class BookingWorkModeProperties {

    private WorkMode workMode = WorkMode.ASYNC;
}
