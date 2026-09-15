package id.my.rascal.invoice.api.event;

import java.time.LocalDateTime;

public record InvoiceDeletedEvent(
    Long invoiceId,
    String invoiceNumber,
    Long diningId,
    LocalDateTime deletedAt
) {}
