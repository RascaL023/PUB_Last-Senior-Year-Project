package id.my.rascal.order.api;

import java.time.LocalDateTime;

public record OrderApiResponse(
    Long id,
    OrderTypeApiResponse orderType,
    String orderNumber,
    Long customerId,
    String customerName,
    Integer totalPrice,
    LocalDateTime createdAt
) {}
