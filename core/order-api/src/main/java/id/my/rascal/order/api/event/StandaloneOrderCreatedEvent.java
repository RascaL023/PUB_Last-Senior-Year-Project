package id.my.rascal.order.api.event;

import java.time.LocalDateTime;
import java.util.List;

import id.my.rascal.order.api.OrderTypeApiResponse;
import id.my.rascal.order.api.event.dto.OrderItemSnapshot;

public record StandaloneOrderCreatedEvent(
    Long orderId,
    String orderNumber,
    Long customerId,
    String customerName,
    OrderTypeApiResponse orderType,
    Integer totalAmount,
    List<OrderItemSnapshot> items,
    LocalDateTime createdAt
    // TODO(customer-module): validasi customerId + snapshot data customer
) {}
