package id.my.rascal.payment.internal.model.response;

import java.time.LocalDateTime;

import id.my.rascal.payment.internal.model.enums.PaymentProvider;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;
import id.my.rascal.payment.internal.model.enums.PaymentTargetType;

public record PaymentResponse(
    Long id,
    PaymentTargetType targetType,
    Long targetId,
    String targetReference,
    PaymentProvider paymentProvider,
    String paymentMethodName,
    String externalId,
    String invoiceUrl,
    PaymentStatus status,
    String paymentChannel,
    String paymentDetail,
    Integer amount,
    LocalDateTime paidAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
