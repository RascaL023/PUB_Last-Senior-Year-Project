package id.my.rascal.dining.internal.model.request;

import java.util.List;

import jakarta.validation.Valid;

public record DiningOrderItemRequest(
    Long menuId,
    Integer quantity,
    List<@Valid DiningOrderItemModifierRequest> modifiers
) {}
