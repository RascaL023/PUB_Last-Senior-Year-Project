package id.my.rascal.order.api;

import java.util.List;

public record OrderItemDetail(
    Long orderItemId,
    Long menuId,
    String itemName,
    Integer unitPrice,
    Integer quantity,
    Integer subtotal,
    List<Modifier> modifiers
) {

    public record Modifier(
        Long modifierOptionId,
        String modifierName,
        Integer additionalPrice
    ) {}

}
