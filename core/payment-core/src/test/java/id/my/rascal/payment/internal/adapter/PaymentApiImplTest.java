package id.my.rascal.payment.internal.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import id.my.rascal.payment.internal.component.PaymentEffect;
import id.my.rascal.payment.internal.component.PaymentStatusFlowPolicy;
import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.repository.PaymentRepository;
import id.my.rascal.payment.internal.service.PaymentEventPublisherService;

// [FIX-T5a] Verifikasi split applied/excess tercatat via direct call
// (pengganti InvoicePaymentAppliedEvent yang hilang di TX-limbo).
class PaymentApiImplTest {

    private PaymentRepository paymentRepository;
    private PaymentApiImpl paymentApi;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        paymentApi = new PaymentApiImpl(
            paymentRepository,
            mock(PaymentEffect.class),
            mock(PaymentStatusFlowPolicy.class),
            mock(PaymentEventPublisherService.class)
        );
    }

    @Test
    void confirmSplit_recordsAppliedAndExcess() {
        Payment payment = new Payment();
        payment.setId(1L);
        when(paymentRepository.findActiveById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        paymentApi.confirmSplit(1L, 30000, 0);

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertEquals(30000, saved.getValue().getAppliedAmount());
        assertEquals(0, saved.getValue().getExcessAmount());
    }

    @Test
    void confirmSplit_recordsExcessParking() {
        Payment payment = new Payment();
        payment.setId(2L);
        when(paymentRepository.findActiveById(2L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        paymentApi.confirmSplit(2L, 18000, 7000);

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertEquals(18000, saved.getValue().getAppliedAmount());
        assertEquals(7000, saved.getValue().getExcessAmount());
    }
}
