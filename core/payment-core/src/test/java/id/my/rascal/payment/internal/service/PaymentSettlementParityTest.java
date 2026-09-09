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

import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.invoice.api.InvoiceApiResponse;
import id.my.rascal.order.api.OrderApi;
import id.my.rascal.dining.api.DiningApi;
import id.my.rascal.payment.api.event.PaymentSettledEvent;
import id.my.rascal.payment.internal.adapter.CashPaymentProcessor;
import id.my.rascal.payment.internal.component.PaymentEffect;
import id.my.rascal.payment.internal.component.PaymentProcessorResolver;
import id.my.rascal.payment.internal.component.PaymentStatusFlowPolicy;
import id.my.rascal.payment.internal.model.enums.PaymentProvider;
import id.my.rascal.payment.internal.model.enums.PaymentTargetType;
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

        paymentService = new PaymentService(
            paymentRepository,
            new PaymentStatusFlowPolicy(),
            new PaymentProcessorResolver(List.of(new CashPaymentProcessor())),
            mock(OrderApi.class),
            mock(DiningApi.class),
            invoiceApi,
            new PaymentEventPublisherService(eventPublisher),
            new PaymentEffect(invoiceApi)
        );
    }

    @Test
    void cashPayment_directSettleAndEventCarryIdenticalFacts() {
        when(invoiceApi.getInvoice(900L)).thenReturn(new InvoiceApiResponse(
            900L, "INV-08092026-AAAAAA", null, "OPEN", 58000, 0, 58000,
            LocalDateTime.now(), LocalDateTime.now(), List.of()
        ));

        paymentService.create(new PaymentRequest(PaymentTargetType.INVOICE, 900L, PaymentProvider.INTERNAL, null));

        ArgumentCaptor<Integer> settledAmount = ArgumentCaptor.forClass(Integer.class);
        verify(invoiceApi).applyPayment(org.mockito.ArgumentMatchers.eq(900L), settledAmount.capture());

        ArgumentCaptor<PaymentSettledEvent> event = ArgumentCaptor.forClass(PaymentSettledEvent.class);
        verify(eventPublisher).publishEvent(event.capture());

        assertEquals(900L, event.getValue().targetId());
        assertEquals("INVOICE", event.getValue().targetType());
        assertEquals(settledAmount.getValue(), event.getValue().settledAmount());
        assertEquals(58000, event.getValue().settledAmount());
    }

}
