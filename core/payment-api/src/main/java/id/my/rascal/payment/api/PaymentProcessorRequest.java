package id.my.rascal.payment.api;

public record PaymentProcessorRequest(
    Integer amount,
    String currency,
    String description,
    String externalId,
    String successRedirectUrl,
    String failureRedirectUrl
) { }

