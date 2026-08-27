package id.my.rascal.dining.internal.model.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import id.my.rascal.order.api.OrderItemApiRequest;

public record CreateDiningOrderRequest(
    @Min(value = 1, message = "Invalid customer ID")
    Long customerId,

    @Size(max = 50, message = "Customer name cannot exceed 50 characters")
    String customerName,

    @Size(max = 255, message = "Notes cannot exceed 255 characters")
    String notes,

    @NotEmpty(message = "Order must have at least 1 item")
    List<@Valid OrderItemApiRequest> items
) {}
