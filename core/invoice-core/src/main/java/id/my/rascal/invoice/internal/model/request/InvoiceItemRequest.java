package id.my.rascal.invoice.internal.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InvoiceItemRequest(
    @Min(value = 1, message = "Invalid order ID")
    Long orderId,

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    String description,

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity,

    @NotNull(message = "Unit price is required")
    @Min(value = 0, message = "Unit price cannot be negative")
    Integer unitPrice
) {}
