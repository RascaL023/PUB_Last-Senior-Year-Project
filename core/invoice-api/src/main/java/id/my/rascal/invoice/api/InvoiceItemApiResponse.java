package id.my.rascal.invoice.api;

public record InvoiceItemApiResponse(
    Long id,
    Long orderItemId,
    Long orderId,
    String description,
    Integer quantity,
    Integer unitPrice,
    Integer amount
) {}
