package id.my.rascal.invoice.api.event;

import java.time.LocalDateTime;

public record InvoiceCreatedEvent(
    Long invoiceId,
    String invoiceNumber,
    Long diningId,
    Integer totalAmount,
    LocalDateTime issuedAt
) {}
