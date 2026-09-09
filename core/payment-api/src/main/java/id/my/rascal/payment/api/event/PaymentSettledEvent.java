package id.my.rascal.payment.api.event;

import java.time.LocalDateTime;

public record PaymentSettledEvent(
    Long paymentId,
    String targetType,
    Long targetId,
    Integer settledAmount,
    String externalId,
    LocalDateTime paidAt
) {}
