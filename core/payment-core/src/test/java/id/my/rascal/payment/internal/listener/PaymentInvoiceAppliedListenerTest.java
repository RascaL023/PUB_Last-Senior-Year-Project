package id.my.rascal.payment.internal.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import id.my.rascal.invoice.api.event.InvoicePaymentAppliedEvent;
import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;
import id.my.rascal.payment.internal.model.enums.PaymentTargetType;
import id.my.rascal.payment.internal.repository.PaymentRepository;

class PaymentInvoiceAppliedListenerTest {

    @Test
    void confirmation_recordsSplitOnPayment() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = new Payment();
        payment.setId(5L);
        payment.setTargetType(PaymentTargetType.INVOICE);
        payment.setTargetId(900L);
        payment.setStatus(PaymentStatus.PAID);
        payment.setAmount(100000);
        when(paymentRepository.findActiveById(5L)).thenReturn(Optional.of(payment));

        new PaymentInvoiceAppliedListener(paymentRepository)
            .onInvoicePaymentApplied(new InvoicePaymentAppliedEvent(5L, 900L, 58000, 42000));

        verify(paymentRepository).save(payment);
        assertEquals(58000, payment.getAppliedAmount());
        assertEquals(42000, payment.getExcessAmount());
    }

}
