package id.my.rascal.order.internal.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemModifierRequest(
    Long id,

    @NotNull(message = "Modifier option ID is required")
    @Min(value = 1, message = "Invalid modifier option ID")
    Long modifierOptionId
) {}
