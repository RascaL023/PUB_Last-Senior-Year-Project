package id.my.rascal.payment.internal.component;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;
import id.my.rascal.payment.internal.model.enums.PaymentTargetType;

@Component
public class PaymentEffect {

    private final InvoiceApi invoiceApi;

    public PaymentEffect(InvoiceApi invoiceApi) {
        this.invoiceApi = invoiceApi;
    }

    public void applyEffectIfPaid(Payment payment, Integer settledAmount) {
        PaymentStatus expectedStatus = PaymentStatus.PAID;
        if (payment.getStatus() != expectedStatus)
            return;

        LocalDateTime now = LocalDateTime.now();
        payment.setPaidAt(now);
        payment.setUpdatedAt(now);

        if (payment.getTargetType() == PaymentTargetType.INVOICE
            && settledAmount != null && settledAmount > 0) {
            invoiceApi.applyPayment(payment.getTargetId(), settledAmount);
        }
    }

}
