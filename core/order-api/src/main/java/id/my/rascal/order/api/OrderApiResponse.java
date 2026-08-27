package id.my.rascal.order.api;

import java.time.LocalDateTime;

public record OrderApiResponse(
    Long id,
    OrderTypeApiResponse orderType,
    String status,
    String paidStatus,
    String orderNumber,
    Long customerId,
    String customerName,
    Long diningId,
    Integer totalPrice,
    LocalDateTime createdAt
) {}
