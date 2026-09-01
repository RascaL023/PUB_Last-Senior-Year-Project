package id.my.rascal.payment.internal.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import id.my.rascal.payment.internal.model.enums.PaymentProvider;
import id.my.rascal.payment.internal.model.enums.PaymentTargetType;

public record PaymentRequest(
    @NotNull(message = "Target type is required")
    PaymentTargetType targetType,

    @NotNull(message = "Target ID is required")
    @Min(value = 1, message = "Invalid target ID")
    Long targetId,

    @NotNull(message = "Payment provider required")
    PaymentProvider paymentProvider,

    @Size(max = 255, message = "Payment detail cannot exceed 255 characters")
    String paymentDetail

) {}
