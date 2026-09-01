package id.my.rascal.payment.api;

public record PaymentProcessorResponse(
    String id,
    String externalId,
    String invoiceUrl,
    String paymentChannel,
    String paymentMethodName,
    PaymentProcessorStatus status,
    Integer amount
) { }
