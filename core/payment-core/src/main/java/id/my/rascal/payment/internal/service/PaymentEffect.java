package id.my.rascal.payment.internal.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.dining.api.DiningApi;
import id.my.rascal.order.api.OrderApi;
import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;

@Service
public class PaymentEffect {

    private final OrderApi orderApi;
    private final DiningApi diningApi;

    public PaymentEffect(
        DiningApi diningApi,
        OrderApi orderApi
    ) {
        this.diningApi = diningApi;
        this.orderApi = orderApi;
    }

    public void applyEffectIfPaid(Payment payment) {
        PaymentStatus expectedStatus = PaymentStatus.PAID;
        if (payment.getStatus() != expectedStatus) 
            return;

        payment.setPaidAt(LocalDateTime.now());
        applyOrderSideEffect(payment);
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
