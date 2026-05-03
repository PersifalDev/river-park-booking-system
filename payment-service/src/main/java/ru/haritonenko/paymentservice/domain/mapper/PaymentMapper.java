package ru.haritonenko.paymentservice.domain.mapper;

import org.springframework.stereotype.Component;
import ru.haritonenko.paymentservice.api.dto.PaymentResponseDto;
import ru.haritonenko.paymentservice.domain.Payment;
import ru.haritonenko.paymentservice.domain.db.entity.PaymentEntity;

@Component
public class PaymentMapper {

    public Payment toDomain(PaymentEntity entity) {
        return new Payment(
                entity.getId(),
                entity.getBookingId(),
                entity.getBookingCode(),
                entity.getUserId(),
                entity.getPriceAmount(),
                entity.getStatus(),
                entity.getPaymentMethod(),
                entity.getPaymentComment(),
                entity.getContactPhone(),
                entity.getPaymentInstruction(),
                entity.getCancellationReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PaymentResponseDto toDto(Payment payment) {
        return new PaymentResponseDto(
                payment.id(),
                payment.bookingId(),
                payment.bookingCode(),
                payment.userId(),
                payment.priceAmount(),
                payment.status(),
                payment.paymentMethod(),
                payment.paymentComment(),
                payment.contactPhone(),
                payment.paymentInstruction(),
                payment.cancellationReason(),
                payment.createdAt(),
                payment.updatedAt()
        );
    }
}
