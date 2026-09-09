package id.my.rascal.invoice.api.event;

import java.time.LocalDateTime;

public record InvoicePaidEvent(
    Long invoiceId,
    String invoiceNumber,
    Long diningId,
    Integer totalAmount,
    Integer paidAmount,
    Integer remainingAmount,
    LocalDateTime paidAt
) {}
