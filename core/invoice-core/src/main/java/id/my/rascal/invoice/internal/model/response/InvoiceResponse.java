package id.my.rascal.invoice.internal.model.response;

import id.my.rascal.invoice.internal.entity.InvoiceStatus;

import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
    Long id,
    String invoiceNumber,
    InvoiceStatus status,
    Integer totalAmount,
    Integer paidAmount,
    Integer remainingAmount,
    LocalDateTime issuedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<InvoiceItemResponse> items
) {}
