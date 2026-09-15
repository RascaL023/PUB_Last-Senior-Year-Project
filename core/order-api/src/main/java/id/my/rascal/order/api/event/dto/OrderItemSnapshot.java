package id.my.rascal.order.api.event.dto;

public record OrderItemSnapshot(
    Long orderItemId,
    Long menuId,
    String itemName,
    Integer quantity,
    Integer unitPrice,
    Integer subtotal
) {}
