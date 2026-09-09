package id.my.rascal.payment.api.event;

import java.time.LocalDateTime;

public record PaymentRefundedEvent(
    Long paymentId,
    String targetType,
    Long targetId,
    Integer refundedAmount,
    String externalId,
    LocalDateTime refundedAt
) {}
