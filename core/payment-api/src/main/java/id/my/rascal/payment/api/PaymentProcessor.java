package id.my.rascal.payment.api;

public interface PaymentProcessor {

    String paymentProvider();
    PaymentProcessorResponse process(PaymentProcessorRequest request);

}
