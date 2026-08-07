package id.my.rascal.order.internal.model.response;

import java.util.List;

public record OrderItemResponse(
    Long id,
    Long menuId,
    String itemName,
    Integer unitPrice,
    Integer quantity,
    Integer subtotal,
    List<OrderItemModifierResponse> modifiers
) {}
