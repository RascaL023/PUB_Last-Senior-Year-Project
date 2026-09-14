package id.my.rascal.payment.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import id.my.rascal.payment.api.event.PaymentSettledEvent;
import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;
import id.my.rascal.payment.internal.model.enums.PaymentTargetType;

class PaymentEventPublisherServiceTest {

    @Test
    void publishSettled_emitsFactWithTargetAndAmount() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        PaymentEventPublisherService publisher = new PaymentEventPublisherService(eventPublisher);

        Payment payment = new Payment();
        payment.setId(5L);
        payment.setTargetType(PaymentTargetType.INVOICE);
        payment.setTargetId(900L);
        payment.setStatus(PaymentStatus.PAID);
        payment.setExternalId("INV-abc");
        payment.setPaidAt(LocalDateTime.now());

        publisher.publishSettled(payment, 58000);

        ArgumentCaptor<PaymentSettledEvent> captor = ArgumentCaptor.forClass(PaymentSettledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        PaymentSettledEvent event = captor.getValue();
        assertEquals(5L, event.paymentId());
        assertEquals("INVOICE", event.targetType());
        assertEquals(900L, event.targetId());
        assertEquals(58000, event.settledAmount());
        assertEquals("INV-abc", event.externalId());
    }

}
