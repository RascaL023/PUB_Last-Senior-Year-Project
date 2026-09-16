package id.my.rascal.payment.internal.component;

import org.springframework.stereotype.Component;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;

@Component
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
            default -> reject("Invalid payment status transition");
        }
    }

    public boolean isTerminal(PaymentStatus status) {
        return status == PaymentStatus.EXPIRED
            || status == PaymentStatus.FAILED;
    }

    private void reject(String message) {
        throw new BadRequestException(message);
    }

}
