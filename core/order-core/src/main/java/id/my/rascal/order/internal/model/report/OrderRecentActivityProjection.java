package id.my.rascal.order.internal.model.report;

import java.time.LocalDateTime;

import id.my.rascal.order.internal.model.enums.OrderPaidStatus;
import id.my.rascal.order.internal.model.enums.OrderStatus;

public record OrderRecentActivityProjection(
    Long orderId,
    String orderNumber,
    OrderStatus status,
    OrderPaidStatus paidStatus,
    Integer totalPrice,
    LocalDateTime createdAt
) {}