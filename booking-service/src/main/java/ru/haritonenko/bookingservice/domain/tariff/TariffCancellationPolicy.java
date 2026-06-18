package ru.haritonenko.bookingservice.domain.tariff;

public enum TariffCancellationPolicy {
    NON_REFUNDABLE,
    FREE_UNTIL_DEADLINE,
    FLEXIBLE,
    STRICT
}
