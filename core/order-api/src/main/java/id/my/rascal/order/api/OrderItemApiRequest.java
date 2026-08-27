package id.my.rascal.order.api;

import java.util.List;

public record OrderItemApiRequest(
    Long menuId,
    Integer quantity,
    List<OrderItemModifierApiRequest> modifiers
) {}
