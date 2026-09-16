package id.my.rascal.payment.api.event;

import java.time.LocalDateTime;

public record PaymentSettledEvent(
    Long paymentId,
    Long invoiceId,
    Integer settledAmount,
    String externalId,
    LocalDateTime paidAt
) {}
