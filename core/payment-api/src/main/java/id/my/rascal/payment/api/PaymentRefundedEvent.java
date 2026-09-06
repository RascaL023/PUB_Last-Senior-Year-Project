package id.my.rascal.payment.api;

import java.time.LocalDateTime;

public record PaymentRefundedEvent(
    Long paymentId,
    long amount,
    LocalDateTime originalPaidAt
) {}
