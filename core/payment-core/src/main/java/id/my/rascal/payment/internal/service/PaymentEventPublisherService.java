package id.my.rascal.payment.internal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import id.my.rascal.payment.api.event.PaymentRefundedEvent;
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
            payment.getTargetType() == null ? null : payment.getTargetType().name(),
            payment.getTargetId(),
            settledAmount,
            payment.getExternalId(),
            payment.getPaidAt()
        ));
        logger.info("Published payment event: settled paymentId={} targetType={} targetId={}",
            payment.getId(), payment.getTargetType(), payment.getTargetId());
    }

    public void publishRefunded(Payment payment) {
        eventPublisher.publishEvent(new PaymentRefundedEvent(
            payment.getId(),
            payment.getTargetType() == null ? null : payment.getTargetType().name(),
            payment.getTargetId(),
            payment.getAmount(),
            payment.getExternalId(),
            payment.getRefundedAt() != null ? payment.getRefundedAt() : payment.getUpdatedAt()
        ));
        logger.info("Published payment event: refunded paymentId={} targetType={} targetId={}",
            payment.getId(), payment.getTargetType(), payment.getTargetId());
    }

}
