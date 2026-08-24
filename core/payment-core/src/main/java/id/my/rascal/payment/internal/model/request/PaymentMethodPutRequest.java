package id.my.rascal.payment.internal.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentMethodPutRequest(
    @NotBlank(message = "Payment method code is required")
    @Size(max = 50, message = "Code cannot exceed 50 characters")
    String code,

    @NotBlank(message = "Payment method name is required")
    @Size(max = 50, message = "Name cannot exceed 50 characters")
    String name,

    Boolean isActive
) {}
