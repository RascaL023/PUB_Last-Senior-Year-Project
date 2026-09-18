package id.my.rascal.order.internal.model.response;

import java.time.LocalDateTime;
import java.util.List;

import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.enums.OrderType;

public record OrderResponse(
    Long id,
    String orderNumber,
    String trackToken,
    OrderStatus status,
    OrderType type,
    Long customerId,
    String customerName,
    String notes,
    Integer totalPrice,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<OrderItemResponse> items
) {}
