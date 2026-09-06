package id.my.rascal.payment.internal.component;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.dining.api.DiningApi;
import id.my.rascal.order.api.OrderApi;
import id.my.rascal.payment.api.PaymentSettledEvent;
import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;

@Component
public class PaymentEffect {

    private final OrderApi orderApi;
    private final DiningApi diningApi;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentEffect(
        DiningApi diningApi,
        OrderApi orderApi,
        ApplicationEventPublisher eventPublisher
    ) {
        this.diningApi = diningApi;
        this.orderApi = orderApi;
        this.eventPublisher = eventPublisher;
    }

    public void applyEffectIfPaid(Payment payment) {
        PaymentStatus expectedStatus = PaymentStatus.PAID;
        if (payment.getStatus() != expectedStatus) 
            return;

        LocalDateTime now = LocalDateTime.now();
        payment.setPaidAt(now);
        payment.setUpdatedAt(now);
        applyOrderSideEffect(payment);
    }

    public void publishSettledEvent(Payment saved) {
        if (saved.getStatus() != PaymentStatus.PAID)
            return;

        eventPublisher.publishEvent(new PaymentSettledEvent(
            saved.getId(),
            saved.getExternalId(),
            saved.getTargetType().name(),
            saved.getTargetId(),
            saved.getAmount(),
            saved.getPaidAt()
        ));
    }
    

    private void applyOrderSideEffect(Payment payment) {
        switch (payment.getTargetType()) {
            case ORDER -> orderApi.markPaid(payment.getTargetId());
            case DINE_IN -> {
                List<Long> orderIds = diningApi.getOrderIds(payment.getTargetId());
                if (orderIds.isEmpty())
                    throw new BadRequestException("There is no orders on " + payment.getTargetType() + " id " + payment.getTargetId());
                orderApi.markPaid(orderIds);
            }
        }
    }

}
