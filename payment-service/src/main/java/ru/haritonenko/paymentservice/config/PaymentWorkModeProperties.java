package ru.haritonenko.paymentservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.haritonenko.commonlibs.communication.WorkMode;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.payment")
public class PaymentWorkModeProperties {

    private WorkMode workMode = WorkMode.ASYNC;
}
