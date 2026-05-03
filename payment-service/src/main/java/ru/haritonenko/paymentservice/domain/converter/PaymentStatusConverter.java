package ru.haritonenko.paymentservice.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.haritonenko.paymentservice.domain.status.PaymentStatus;

@Converter(autoApply = true)
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, String> {

    @Override
    public String convertToDatabaseColumn(PaymentStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public PaymentStatus convertToEntityAttribute(String dbData) {
        return dbData == null || dbData.isBlank() ? null : PaymentStatus.valueOf(dbData);
    }
}
