package id.my.rascal.payment.internal.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import id.my.rascal.payment.api.PaymentApiWebhookRequest;
import id.my.rascal.payment.api.PaymentProcessorStatus;
import id.my.rascal.payment.api.event.PaymentSettledEvent;
import id.my.rascal.payment.internal.component.PaymentEffect;
import id.my.rascal.payment.internal.component.PaymentStatusFlowPolicy;
import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;
import id.my.rascal.payment.internal.model.enums.PaymentTargetType;
import id.my.rascal.payment.internal.repository.PaymentRepository;
import id.my.rascal.payment.internal.service.PaymentEventPublisherService;

class PaymentWebhookParityTest {

    @Test
    void webhookPaid_publishesSettledEventWithPayloadFacts() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = new Payment();
        payment.setId(5L);
        payment.setTargetType(PaymentTargetType.INVOICE);
        payment.setTargetId(900L);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(58000);
        payment.setExternalId("INV-abc");
        when(paymentRepository.findByExternalId("INV-abc")).thenReturn(Optional.of(payment));

        PaymentApiImpl handler = new PaymentApiImpl(
            paymentRepository,
            new PaymentEffect(),
            new PaymentStatusFlowPolicy(),
            new PaymentEventPublisherService(eventPublisher)
        );

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

}
