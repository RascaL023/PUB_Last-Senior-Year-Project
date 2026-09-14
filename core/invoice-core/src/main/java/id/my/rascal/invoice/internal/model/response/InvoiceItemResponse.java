package id.my.rascal.invoice.internal.model.response;

public record InvoiceItemResponse(
    Long id,
    Long orderItemId,
    Long orderId,
    String description,
    Integer quantity,
    Integer unitPrice,
    Integer amount,
    Boolean refunded
) {}
