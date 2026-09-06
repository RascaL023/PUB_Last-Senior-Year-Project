package id.my.rascal.payment.api;

import java.time.LocalDateTime;

public record PaymentSettledEvent(
    Long paymentId,
    String externalId,
    String targetType,
    Long targetId,
    long amount,
    LocalDateTime paidAt
) {}
