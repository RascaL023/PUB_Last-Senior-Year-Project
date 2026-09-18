package id.my.rascal.order.internal.model.response;

import java.time.LocalDateTime;
import java.util.List;

import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.enums.OrderType;

public record KitchenTicketResponse(
    Long orderId,
    String orderNumber,
    OrderType type,
    OrderStatus status,
    String tableNumber,
    String notes,
    LocalDateTime createdAt,
    List<OrderItemResponse> items
) {}
