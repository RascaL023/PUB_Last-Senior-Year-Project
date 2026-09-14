package id.my.rascal.invoice.api.event;

import java.time.LocalDateTime;

public record InvoiceVoidedEvent(
    Long invoiceId,
    String invoiceNumber,
    Long diningId,
    LocalDateTime voidedAt
) {}
