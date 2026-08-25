package id.my.rascal.order.internal.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

import id.my.rascal.order.internal.model.enums.OrderType;

public record OrderPutRequest(
    @Min(value = 1, message = "Invalid customer ID")
    Long customerId,

    @Size(max = 50, message = "Customer name cannot exceed 50 characters")
    String customerName,

    @Size(max = 255, message = "Notes cannot exceed 255 characters")
    String notes,

    @NotNull(message = "Order type can't be null")
    OrderType type,

    @NotEmpty(message = "Order must have at least 1 item")
    List<@Valid OrderItemRequest> items
) {}
