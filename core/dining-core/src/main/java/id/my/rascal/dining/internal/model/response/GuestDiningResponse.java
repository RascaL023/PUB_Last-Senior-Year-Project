package id.my.rascal.dining.internal.model.response;

import java.util.List;

public record GuestDiningResponse(
    String tableNumber,
    String status,
    Integer totalPrice,
    String invoiceStatus,
    List<GuestOrderSummary> orders
) {}
