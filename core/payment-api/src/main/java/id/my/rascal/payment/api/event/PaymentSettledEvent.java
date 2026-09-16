package id.my.rascal.payment.api.event;

import java.time.LocalDateTime;

/**
 * Payment selalu menarget satu invoice — tidak ada lagi targetType/targetId generik.
 */
public record PaymentSettledEvent(
    Long paymentId,
    Long invoiceId,
    Integer settledAmount,
    String externalId,
    LocalDateTime paidAt
) {}
