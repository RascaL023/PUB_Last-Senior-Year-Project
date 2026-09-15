package id.my.rascal.order.internal.model.report;

import java.time.LocalDateTime;

import id.my.rascal.order.internal.model.enums.OrderStatus;

public record OrderRecentActivityProjection(
    Long orderId,
    String orderNumber,
    OrderStatus status,
    Integer totalPrice,
    LocalDateTime createdAt
) {}
