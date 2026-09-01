package id.my.rascal.payment.internal.adapter;

import org.springframework.stereotype.Component;

import id.my.rascal.payment.api.PaymentProcessor;
import id.my.rascal.payment.api.PaymentProcessorRequest;
import id.my.rascal.payment.api.PaymentProcessorResponse;
import id.my.rascal.payment.api.PaymentProcessorStatus;

@Component
public class CashPaymentProcessor implements PaymentProcessor {

    @Override
    public String paymentProvider() {
        return "INTERNAL";
    }

    @Override
    public PaymentProcessorResponse process(PaymentProcessorRequest request) {
        return new PaymentProcessorResponse(
            null, null, null, 
            "INTERNAL_CASH",
            "CASH",
            PaymentProcessorStatus.PAID,
            request.amount()
        );
    }

}
