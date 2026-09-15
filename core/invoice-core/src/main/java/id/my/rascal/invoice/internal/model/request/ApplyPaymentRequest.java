package id.my.rascal.invoice.internal.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ApplyPaymentRequest(
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be greater than 0")
    Integer amount
) {}
