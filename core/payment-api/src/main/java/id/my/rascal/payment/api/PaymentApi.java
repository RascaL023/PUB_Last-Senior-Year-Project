package id.my.rascal.payment.api;

public interface PaymentApi {

    void handleWebhookRequest(PaymentApiWebhookRequest payload, String raw);
    void confirmSplit(Long paymentId, Integer appliedAmount, Integer excessAmount);

}
