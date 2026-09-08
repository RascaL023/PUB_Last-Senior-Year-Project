package id.my.rascal.dining.api.event;

import java.util.List;

import id.my.rascal.order.api.event.dto.OrderItemSnapshot;

public record DiningOrderAddedEvent(
    Long diningId,
    Long orderId,
    List<OrderItemSnapshot> items
) {}
