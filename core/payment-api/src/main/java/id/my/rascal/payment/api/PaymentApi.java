package id.my.rascal.payment.api;

public interface PaymentApi {

    void handleWeebhookRequest(PaymentApiWebhookRequest payload, String raw);

}
