package id.my.rascal.xendit.internal.adapter;

import org.springframework.stereotype.Component;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.payment.api.PaymentProcessor;
import id.my.rascal.payment.api.PaymentProcessorRequest;
import id.my.rascal.payment.api.PaymentProcessorResponse;
import id.my.rascal.xendit.internal.exception.XenditClientException;
import id.my.rascal.xendit.internal.service.XenditService;

@Component
public class XenditPaymentProcessor implements PaymentProcessor {

    private final XenditService xenditService;

    public XenditPaymentProcessor(XenditService xenditService) {
        this.xenditService = xenditService;
    }

    @Override
    public String paymentProvider() {
        return "XENDIT";
    }

    @Override
    public PaymentProcessorResponse process(PaymentProcessorRequest request) {
        try {
            return xenditService.initPayment(request);
        } catch (XenditClientException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

}
