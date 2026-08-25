package id.my.rascal.order.api;

import java.time.LocalDateTime;

public record OrderSnapshot(
    Long id,
    OrderTypeSnapshot orderType,
    String orderNumber,
    Long customerId,
    String customerName,
    Integer totalPrice,
    LocalDateTime createdAt
) {}
