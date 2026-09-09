package id.my.rascal.invoice.internal.listener;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.my.rascal.invoice.internal.entity.InvoiceStatus;
import id.my.rascal.invoice.internal.model.response.InvoiceItemResponse;
import id.my.rascal.invoice.internal.model.response.InvoiceResponse;
import id.my.rascal.invoice.internal.service.InvoiceQueryService;
import id.my.rascal.invoice.internal.service.InvoiceService;
import id.my.rascal.payment.api.event.PaymentSettledEvent;

class InvoicePaymentEventListenerTest {

    private InvoiceService invoiceService;
    private InvoiceQueryService invoiceQueryService;
    private InvoicePaymentEventListener listener;

    @BeforeEach
    void setUp() {
        invoiceService = mock(InvoiceService.class);
        invoiceQueryService = mock(InvoiceQueryService.class);
        listener = new InvoicePaymentEventListener(invoiceService, invoiceQueryService);
    }

    @Test
    void invoiceTarget_appliesPayment() {
        when(invoiceQueryService.findActiveInvoiceById(900L)).thenReturn(openInvoice(900L, 58000, 0));

        listener.onPaymentSettled(new PaymentSettledEvent(
            5L, "INVOICE", 900L, 58000, "INV-abc", LocalDateTime.now()
        ));

        verify(invoiceService).applyPayment(Long.valueOf(900L), Integer.valueOf(58000));
    }

    @Test
    void legacyTarget_isIgnored() {
        listener.onPaymentSettled(new PaymentSettledEvent(
            5L, "ORDER", 101L, 58000, "INV-abc", LocalDateTime.now()
        ));

        verify(invoiceService, never()).applyPayment(anyLong(), anyInt());
        verify(invoiceQueryService, never()).findActiveInvoiceById(anyLong());
    }

    @Test
    void replayedSettlement_isSkipped() {
        when(invoiceQueryService.findActiveInvoiceById(900L)).thenReturn(paidInvoice(900L, 58000));

        listener.onPaymentSettled(new PaymentSettledEvent(
            5L, "INVOICE", 900L, 58000, "INV-abc", LocalDateTime.now()
        ));

        verify(invoiceService, never()).applyPayment(anyLong(), anyInt());
    }

    private InvoiceResponse openInvoice(Long id, int total, int paid) {
        return new InvoiceResponse(
            id, "INV-08092026-AAAAAA", null, InvoiceStatus.OPEN,
            total, paid, total - paid,
            LocalDateTime.now(), LocalDateTime.now(), null,
            List.of(new InvoiceItemResponse(1L, 11L, 101L, "Nasi Goreng", 2, 25000, 50000))
        );
    }

    private InvoiceResponse paidInvoice(Long id, int total) {
        return new InvoiceResponse(
            id, "INV-08092026-AAAAAA", null, InvoiceStatus.PAID,
            total, total, 0,
            LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(),
            List.of(new InvoiceItemResponse(1L, 11L, 101L, "Nasi Goreng", 2, 25000, 50000))
        );
    }

}
