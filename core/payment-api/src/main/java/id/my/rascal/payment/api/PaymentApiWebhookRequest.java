package id.my.rascal.payment.api;

public record PaymentApiWebhookRequest(
    // String id,
    String externalId,
    PaymentProcessorStatus status,
    // Integer amount,
    Integer paidAmount,
    String paymentMethod,
    String paymentChannel,
    String currency
    // String bankCode,
    // String payerEmail,
    // String description,
    // Integer adjustedReceivedAmount,
    // Integer feesPaidAmount,
    // String paymentDestination,
    // String userId,
    // Boolean isHigh,
    // String created,
    // String updated,
    // String merchantName,
    // String paymentId,
    // String paymentMethodId,
    // Object paymentDetails
) {}
