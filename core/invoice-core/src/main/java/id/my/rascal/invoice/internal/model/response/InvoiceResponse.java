package id.my.rascal.invoice.internal.model.response;

import id.my.rascal.invoice.internal.entity.InvoiceStatus;

import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
    Long id,
    String invoiceNumber,
    Long diningId,
    InvoiceStatus status,
    Integer totalAmount,
    Integer paidAmount,
    Integer remainingAmount,
    LocalDateTime issuedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long customerId,
    String customerName,
    List<InvoiceItemResponse> items
) {}
