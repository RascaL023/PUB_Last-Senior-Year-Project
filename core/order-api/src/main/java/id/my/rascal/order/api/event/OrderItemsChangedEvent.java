package id.my.rascal.order.api.event;

import java.time.LocalDateTime;
import java.util.List;

import id.my.rascal.order.api.event.dto.OrderItemSnapshot;

public record OrderItemsChangedEvent(
    Long orderId,
    String orderNumber,
    List<OrderItemSnapshot> items,
    LocalDateTime updatedAt
) {}
