package id.my.rascal.payment.internal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import id.my.rascal.payment.api.event.PaymentSettledEvent;
import id.my.rascal.payment.internal.entity.Payment;

@Service
public class PaymentEventPublisherService {

    private final ApplicationEventPublisher eventPublisher;
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventPublisherService.class);

    public PaymentEventPublisherService(
        ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = eventPublisher;
    }

    public void publishSettled(Payment payment, Integer settledAmount) {
        eventPublisher.publishEvent(new PaymentSettledEvent(
            payment.getId(),
            payment.getInvoiceId(),
            settledAmount,
            payment.getExternalId(),
            payment.getPaidAt()
        ));
        logger.info("Published payment event: settled paymentId={} invoiceId={}",
            payment.getId(), payment.getInvoiceId());
    }

}
