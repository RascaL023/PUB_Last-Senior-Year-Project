package id.my.rascal.order.internal.model.response;

import id.my.rascal.order.internal.model.enums.OrderStatus;
// import id.my.rascal.order.internal.model.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    String orderNumber,
    OrderStatus status,
    // PaymentStatus paymentStatus,
    Long customerId,
    String customerName,
    String notes,
    Integer totalPrice,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<OrderItemResponse> items
) {}
