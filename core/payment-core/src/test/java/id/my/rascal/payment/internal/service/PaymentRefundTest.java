package id.my.rascal.payment.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.invoice.api.InvoiceApiResponse;
import id.my.rascal.invoice.api.InvoiceItemApiResponse;
import id.my.rascal.payment.internal.adapter.CashPaymentProcessor;
import id.my.rascal.payment.internal.component.PaymentEffect;
import id.my.rascal.payment.internal.component.PaymentProcessorResolver;
import id.my.rascal.payment.internal.component.PaymentStatusFlowPolicy;
import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;
import id.my.rascal.payment.internal.model.enums.PaymentTargetType;
import id.my.rascal.payment.internal.model.request.PaymentRefundRequest;
import id.my.rascal.payment.internal.repository.PaymentRepository;

class PaymentRefundTest {

    private PaymentRepository paymentRepository;
    private InvoiceApi invoiceApi;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        invoiceApi = mock(InvoiceApi.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));

        paymentService = new PaymentService(
            paymentRepository,
            new PaymentStatusFlowPolicy(),
            new PaymentProcessorResolver(List.of(new CashPaymentProcessor())),
            invoiceApi,
            new PaymentEventPublisherService(eventPublisher),
            new PaymentEffect(),
            new TransactionTemplate(transactionManager)
        );
    }

    @Test
    void refundPayment_withoutItems_fullRefundAdjustsInvoice() {
        Payment payment = paidPayment(5L, 900L, 58000);
        when(paymentRepository.findActiveById(5L)).thenReturn(Optional.of(payment));
        when(invoiceApi.getInvoice(900L)).thenReturn(new InvoiceApiResponse(
            900L, "INV-1", null, "PAID", 58000, 58000, 0,
            LocalDateTime.now(), LocalDateTime.now(),
            List.of(
                new InvoiceItemApiResponse(1L, 11L, 101L, "A", 1, 30000, 30000, false),
                new InvoiceItemApiResponse(2L, 12L, 101L, "B", 1, 28000, 28000, false)
            )
        ));

        paymentService.markRefunded(5L, null);

        verify(invoiceApi).refundItems(eq(900L), eq(List.of(11L, 12L)), eq(5L));
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
    }

    @Test
    void refundPartialPayment_invoiceAndPaymentConsistent() {
        Payment payment = paidPayment(5L, 900L, 58000);
        when(paymentRepository.findActiveById(5L)).thenReturn(Optional.of(payment));

        paymentService.markRefunded(5L, new PaymentRefundRequest(List.of(12L)));

        verify(invoiceApi).refundItems(eq(900L), eq(List.of(12L)), eq(5L));
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
    }

    private Payment paidPayment(Long id, Long invoiceId, int amount) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setTargetType(PaymentTargetType.INVOICE);
        payment.setTargetId(invoiceId);
        payment.setStatus(PaymentStatus.PAID);
        payment.setAmount(amount);
        return payment;
    }
}
