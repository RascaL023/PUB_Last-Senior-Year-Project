package id.my.rascal.payment.internal.component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.payment.api.PaymentProcessor;

@Component
public class PaymentProcessorResolver {

    private final Map<String, PaymentProcessor> processors;

    public PaymentProcessorResolver(
        List<PaymentProcessor> processors
    ) {
        this.processors = processors.stream()
            .collect(Collectors.toMap(
                PaymentProcessor::paymentProvider,
                Function.identity()
            ));
    }

    public PaymentProcessor resolve(String paymentProvider) {
        PaymentProcessor processor = processors.get(paymentProvider);
        if (processor == null) {
            test();
            throw new BadRequestException("Unsupported payment method: " + paymentProvider);
        }

        return processor;
    }

    private void test() {
        for (String provider : this.processors.keySet()) {
            System.out.println(provider);
        }
    }

}
