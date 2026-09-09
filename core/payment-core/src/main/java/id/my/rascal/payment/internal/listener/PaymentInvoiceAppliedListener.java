package id.my.rascal.payment.internal.listener;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.invoice.api.event.InvoicePaymentAppliedEvent;
import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.repository.PaymentRepository;

@Component
public class PaymentInvoiceAppliedListener {

    private static final Logger logger = LoggerFactory.getLogger(PaymentInvoiceAppliedListener.class);

    private final PaymentRepository paymentRepository;

    public PaymentInvoiceAppliedListener(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvoicePaymentApplied(InvoicePaymentAppliedEvent event) {
        Payment payment = paymentRepository.findActiveById(event.paymentId())
            .orElseThrow(() -> new NotFoundException("Payment not found with id: " + event.paymentId()));

        payment.setAppliedAmount(event.appliedAmount());
        payment.setExcessAmount(event.excessAmount());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        logger.info("Recorded settlement split: paymentId={} applied={} excess={}",
            event.paymentId(), event.appliedAmount(), event.excessAmount());
    }

}
