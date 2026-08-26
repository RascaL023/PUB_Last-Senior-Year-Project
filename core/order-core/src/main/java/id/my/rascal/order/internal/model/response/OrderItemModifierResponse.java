package id.my.rascal.order.internal.model.response;

public record OrderItemModifierResponse(
    Long id,
    Long modifierTypeId,
    Long modifierOptionId,
    String modifierName,
    Integer additionalPrice
) {}
