package id.my.rascal.payment.internal.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import id.my.rascal.payment.api.PaymentApiWebhookRequest;
import id.my.rascal.payment.api.PaymentProcessorStatus;
import id.my.rascal.payment.api.event.PaymentRefundedEvent;
import id.my.rascal.payment.api.event.PaymentSettledEvent;
import id.my.rascal.payment.internal.component.PaymentEffect;
import id.my.rascal.payment.internal.component.PaymentStatusFlowPolicy;
import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;
import id.my.rascal.payment.internal.model.enums.PaymentTargetType;
import id.my.rascal.payment.internal.repository.PaymentRepository;
import id.my.rascal.payment.internal.service.PaymentEventPublisherService;

class PaymentWebhookParityTest {

    private PaymentRepository paymentRepository;
    private ApplicationEventPublisher eventPublisher;
    private PaymentApiImpl handler;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler = new PaymentApiImpl(
            paymentRepository,
            new PaymentEffect(),
            new PaymentStatusFlowPolicy(),
            new PaymentEventPublisherService(eventPublisher)
        );
    }

    private Payment pendingPayment() {
        Payment payment = new Payment();
        payment.setId(5L);
        payment.setTargetType(PaymentTargetType.INVOICE);
        payment.setTargetId(900L);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(58000);
        payment.setExternalId("INV-abc");
        return payment;
    }

    @Test
    void webhookPaid_publishesSettledEventWithPayloadFacts() {
        when(paymentRepository.findByExternalId("INV-abc")).thenReturn(Optional.of(pendingPayment()));

        handler.handleWeebhookRequest(new PaymentApiWebhookRequest(
            "INV-abc", PaymentProcessorStatus.PAID, 58000, "CASH", "INTERNAL_CASH", "IDR"
        ), "{}");

        ArgumentCaptor<PaymentSettledEvent> event = ArgumentCaptor.forClass(PaymentSettledEvent.class);
        verify(eventPublisher).publishEvent(event.capture());

        assertEquals(5L, event.getValue().paymentId());
        assertEquals(900L, event.getValue().targetId());
        assertEquals("INVOICE", event.getValue().targetType());
        assertEquals(58000, event.getValue().settledAmount());
    }

    @Test
    void webhookWithoutPaidAmount_keepsRecordedAmount() {
        when(paymentRepository.findByExternalId("INV-abc")).thenReturn(Optional.of(pendingPayment()));

        handler.handleWeebhookRequest(new PaymentApiWebhookRequest(
            "INV-abc", PaymentProcessorStatus.EXPIRED, null, null, null, "IDR"
        ), "{}");

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertEquals(58000, saved.getValue().getAmount());
        assertEquals(PaymentStatus.EXPIRED, saved.getValue().getStatus());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void staleWebhookOnTerminalPayment_acknowledgedWithoutEffect() {
        Payment paid = pendingPayment();
        paid.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findByExternalId("INV-abc")).thenReturn(Optional.of(paid));

        handler.handleWeebhookRequest(new PaymentApiWebhookRequest(
            "INV-abc", PaymentProcessorStatus.EXPIRED, null, null, null, "IDR"
        ), "{}");

        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void webhookRefundedFromPaid_transitionsAndPublishes() {
        Payment paid = pendingPayment();
        paid.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findByExternalId("INV-abc")).thenReturn(Optional.of(paid));

        handler.handleWeebhookRequest(new PaymentApiWebhookRequest(
            "INV-abc", PaymentProcessorStatus.REFUNDED, 58000, "CASH", "INTERNAL_CASH", "IDR"
        ), "{}");

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertEquals(PaymentStatus.REFUNDED, saved.getValue().getStatus());

        ArgumentCaptor<PaymentRefundedEvent> event = ArgumentCaptor.forClass(PaymentRefundedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(5L, event.getValue().paymentId());
    }

}
