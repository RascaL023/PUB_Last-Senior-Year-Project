package id.my.rascal.payment.internal.service;

import org.springframework.stereotype.Service;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;

@Service
public class PaymentStatusFlowPolicy {

    public void validateFlow(PaymentStatus oldStatus, PaymentStatus newStatus) {
        if (oldStatus == newStatus) return;

        if (isTerminal(oldStatus)) 
            reject("Payment with status " + oldStatus + " cannot be changed");

        switch (newStatus) {
            case PAID -> {
                if (oldStatus != PaymentStatus.PENDING)
                    reject("Only PENDING payment can become PAID");
            }
            case EXPIRED, FAILED -> {
                if (oldStatus != PaymentStatus.PENDING)
                    reject("Only PENDING payment can become " + newStatus);
            }
            case REFUNDED -> {
                if (oldStatus != PaymentStatus.PAID)
                    reject("Only PAID payment can be REFUNDED");
            }
            default -> reject("Invalid payment status transition");
        }
    }

    private boolean isTerminal(PaymentStatus status) {
        return status == PaymentStatus.PAID
            || status == PaymentStatus.EXPIRED
            || status == PaymentStatus.FAILED
            || status == PaymentStatus.REFUNDED;
    }

    private void reject(String message) {
        throw new BadRequestException(message);
    }
}
