package id.my.rascal.dining.internal.model.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record GuestOrderRequest(
    @Size(max = 50, message = "Customer name cannot exceed 50 characters")
    String customerName,

    @Size(max = 255, message = "Notes cannot exceed 255 characters")
    String notes,

    @NotEmpty(message = "Order must have at least 1 item")
    List<@Valid DiningOrderItemRequest> items
) {}
