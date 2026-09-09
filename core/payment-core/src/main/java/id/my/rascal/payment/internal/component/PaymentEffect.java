package id.my.rascal.payment.internal.component;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;

@Component
public class PaymentEffect {

    public void applyEffectIfPaid(Payment payment) {
        PaymentStatus expectedStatus = PaymentStatus.PAID;
        if (payment.getStatus() != expectedStatus)
            return;

        LocalDateTime now = LocalDateTime.now();
        payment.setPaidAt(now);
        payment.setUpdatedAt(now);
    }

}
