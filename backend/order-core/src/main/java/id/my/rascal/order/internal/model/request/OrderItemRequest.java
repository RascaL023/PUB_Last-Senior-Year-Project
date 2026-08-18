package id.my.rascal.order.internal.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderItemRequest(
    Long id,

    @NotNull(message = "Menu ID is required")
    @Min(value = 1, message = "Invalid menu ID")
    Long menuId,

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity,

    @Valid List<OrderItemModifierRequest> modifiers
) {}
