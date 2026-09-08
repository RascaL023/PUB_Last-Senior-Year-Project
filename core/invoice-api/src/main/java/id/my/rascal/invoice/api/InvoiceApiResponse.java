package id.my.rascal.invoice.api;

import java.time.LocalDateTime;
import java.util.List;

public record InvoiceApiResponse(
    Long id,
    String invoiceNumber,
    Long diningId,
    String status,
    Integer totalAmount,
    Integer paidAmount,
    Integer remainingAmount,
    LocalDateTime issuedAt,
    LocalDateTime createdAt,
    List<InvoiceItemApiResponse> items
) {}
