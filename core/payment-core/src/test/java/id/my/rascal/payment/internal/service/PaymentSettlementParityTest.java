package id.my.rascal.payment.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.invoice.api.InvoiceApiResponse;
import id.my.rascal.payment.api.PaymentProcessor;
import id.my.rascal.payment.api.PaymentProcessorRequest;
import id.my.rascal.payment.api.PaymentProcessorResponse;
import id.my.rascal.payment.api.event.PaymentSettledEvent;
import id.my.rascal.payment.internal.adapter.CashPaymentProcessor;
import id.my.rascal.payment.internal.component.PaymentEffect;
import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.component.PaymentProcessorResolver;
import id.my.rascal.payment.internal.component.PaymentStatusFlowPolicy;
import id.my.rascal.payment.internal.model.enums.PaymentProvider;
import id.my.rascal.payment.internal.model.request.PaymentRequest;
import id.my.rascal.payment.internal.repository.PaymentRepository;

class PaymentSettlementParityTest {

    private PaymentRepository paymentRepository;
    private InvoiceApi invoiceApi;
    private ApplicationEventPublisher eventPublisher;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        invoiceApi = mock(InvoiceApi.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
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
    void cashPayment_publishesSettledEventWithTargetFacts() {
        when(invoiceApi.getInvoice(900L)).thenReturn(new InvoiceApiResponse(
            900L, "INV-08092026-AAAAAA", null, "OPEN", 58000, 0, 58000,
            LocalDateTime.now(), LocalDateTime.now(), null, null, List.of()
        ));
        when(paymentRepository.existsActivePendingByInvoiceId(900L)).thenReturn(false);

        paymentService.create(new PaymentRequest(900L, PaymentProvider.INTERNAL, null, null));

        ArgumentCaptor<PaymentSettledEvent> event = ArgumentCaptor.forClass(PaymentSettledEvent.class);
        verify(eventPublisher).publishEvent(event.capture());

        assertEquals(900L, event.getValue().invoiceId());
        assertEquals(58000, event.getValue().settledAmount());
    }

    // [FIX-T7a] Payment untuk invoice yang sudah lunas harus ditolak,
    // bukan menciptakan record PAID Rp0.
    @Test
    void paidInvoice_rejectsNewPayment() {
        when(invoiceApi.getInvoice(900L)).thenReturn(new InvoiceApiResponse(
            900L, "INV-08092026-AAAAAA", null, "PAID", 58000, 58000, 0,
            LocalDateTime.now(), LocalDateTime.now(), null, null, List.of()
        ));
        when(paymentRepository.existsActivePendingByInvoiceId(900L)).thenReturn(false);

        org.junit.jupiter.api.Assertions.assertThrows(
            id.my.rascal.common.exception.BadRequestException.class,
            () -> paymentService.create(new PaymentRequest(900L, PaymentProvider.INTERNAL, null, null))
        );
    }

    // [B2] Partial pay: amount opsional membatasi nominal yang ditagih.
    @Test
    void partialPayment_billsRequestedAmountNotFullRemaining() {
        when(invoiceApi.getInvoice(900L)).thenReturn(new InvoiceApiResponse(
            900L, "INV-08092026-AAAAAA", null, "PARTIALLY_PAID", 50000, 20000, 30000,
            LocalDateTime.now(), LocalDateTime.now(), null, null, List.of()
        ));
        when(paymentRepository.existsActivePendingByInvoiceId(900L)).thenReturn(false);

        paymentService.create(new PaymentRequest(900L, PaymentProvider.INTERNAL, null, 10000));

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertEquals(10000, saved.getValue().getAmount());
        assertEquals(900L, saved.getValue().getInvoiceId());
    }

    // [B2] Partial pay: amount melebihi sisa tagihan ditolak, bukan diparkir.
    @Test
    void partialPayment_overRemaining_rejected() {
        when(invoiceApi.getInvoice(900L)).thenReturn(new InvoiceApiResponse(
            900L, "INV-08092026-AAAAAA", null, "PARTIALLY_PAID", 50000, 20000, 30000,
            LocalDateTime.now(), LocalDateTime.now(), null, null, List.of()
        ));
        when(paymentRepository.existsActivePendingByInvoiceId(900L)).thenReturn(false);

        org.junit.jupiter.api.Assertions.assertThrows(
            id.my.rascal.common.exception.BadRequestException.class,
            () -> paymentService.create(new PaymentRequest(900L, PaymentProvider.INTERNAL, null, 30001))
        );
    }

    // [B2] Satu payment PENDING aktif per invoice — dua QRIS tidak boleh menagih tagihan yang sama.
    @Test
    void pendingPaymentActive_rejectsNewPayment() {
        when(paymentRepository.existsActivePendingByInvoiceId(900L)).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertThrows(
            id.my.rascal.common.exception.BadRequestException.class,
            () -> paymentService.create(new PaymentRequest(900L, PaymentProvider.XENDIT, null, null))
        );
    }

    // Masking: kegagalan infrastruktur processor TIDAK boleh dibungkus
    // BadRequestException (status 400 + pesan mentah ke FE). Exception naik
    // apa adanya -> catch-all handler yang me-log dengan stacktrace dan
    // membalas 500 generik.
    @Test
    void processorInfrastructureFailure_propagatesUnwrappedNotAsBadRequest() {
        when(invoiceApi.getInvoice(900L)).thenReturn(new InvoiceApiResponse(
            900L, "INV-08092026-AAAAAA", null, "OPEN", 50000, 0, 50000,
            LocalDateTime.now(), LocalDateTime.now(), null, null, List.of()
        ));
        when(paymentRepository.existsActivePendingByInvoiceId(900L)).thenReturn(false);

        PaymentProcessor failing = new PaymentProcessor() {
            @Override
            public String paymentProvider() {
                return "XENDIT";
            }

            @Override
            public PaymentProcessorResponse process(PaymentProcessorRequest request) {
                throw new RuntimeException("db connection lost");
            }
        };
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        paymentService = new PaymentService(
            paymentRepository,
            new PaymentStatusFlowPolicy(),
            new PaymentProcessorResolver(List.of(failing)),
            invoiceApi,
            new PaymentEventPublisherService(eventPublisher),
            new PaymentEffect(),
            new TransactionTemplate(txManager)
        );

        RuntimeException thrown = org.junit.jupiter.api.Assertions.assertThrows(
            RuntimeException.class,
            () -> paymentService.create(new PaymentRequest(900L, PaymentProvider.XENDIT, null, null))
        );
        org.junit.jupiter.api.Assertions.assertEquals("db connection lost", thrown.getMessage());
        org.junit.jupiter.api.Assertions.assertFalse(
            thrown instanceof id.my.rascal.common.exception.BadRequestException);
        verify(paymentRepository, org.mockito.Mockito.never()).save(any());
    }

}
