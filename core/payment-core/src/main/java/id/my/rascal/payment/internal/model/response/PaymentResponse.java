package id.my.rascal.payment.internal.model.response;

import java.time.LocalDateTime;

import id.my.rascal.payment.internal.model.enums.PaymentProvider;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;

public record PaymentResponse(
    Long id,
    Long invoiceId,
    String invoiceNumber,
    PaymentProvider paymentProvider,
    String paymentMethodName,
    String externalId,
    String invoiceUrl,
    PaymentStatus status,
    String paymentChannel,
    String paymentDetail,
    Integer amount,
    Integer appliedAmount,
    Integer excessAmount,
    LocalDateTime paidAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
