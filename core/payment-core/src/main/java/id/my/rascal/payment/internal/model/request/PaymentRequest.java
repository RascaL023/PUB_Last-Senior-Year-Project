package id.my.rascal.payment.internal.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import id.my.rascal.payment.internal.model.enums.PaymentTargetType;

public record PaymentRequest(
    @NotNull(message = "Target type is required")
    PaymentTargetType targetType,

    @NotNull(message = "Target ID is required")
    @Min(value = 1, message = "Invalid target ID")
    Long targetId,

    @NotNull(message = "Payment method ID is required")
    @Min(value = 1, message = "Invalid payment method ID")
    Long paymentMethodId,

    @Size(max = 50, message = "Payment channel cannot exceed 50 characters")
    String paymentChannel,

    @Size(max = 255, message = "Payment detail cannot exceed 255 characters")
    String paymentDetail,

    @Size(max = 255, message = "External ID cannot exceed 255 characters")
    String externalId,

    @Size(max = 512, message = "Invoice URL cannot exceed 512 characters")
    String invoiceUrl
) {}
