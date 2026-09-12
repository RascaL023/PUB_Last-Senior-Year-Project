package id.my.rascal.invoice.internal.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.mockito.ArgumentCaptor;

import id.my.rascal.invoice.internal.entity.InvoiceStatus;
import id.my.rascal.invoice.internal.model.response.InvoiceItemResponse;
import id.my.rascal.invoice.internal.model.response.InvoiceResponse;
import id.my.rascal.invoice.internal.service.InvoiceQueryService;
import id.my.rascal.invoice.internal.service.InvoiceService;
import id.my.rascal.payment.api.PaymentApi;
import id.my.rascal.payment.api.event.PaymentSettledEvent;

class InvoicePaymentEventListenerTest {

    private InvoiceService invoiceService;
    private InvoiceQueryService invoiceQueryService;
    private PaymentApi paymentApi;
    private InvoicePaymentEventListener listener;

    @BeforeEach
    void setUp() {
        invoiceService = mock(InvoiceService.class);
        invoiceQueryService = mock(InvoiceQueryService.class);
        paymentApi = mock(PaymentApi.class);
        listener = new InvoicePaymentEventListener(invoiceService, invoiceQueryService, paymentApi);
    }

    @Test
    void invoiceTarget_appliesFullSettlement() {
        when(invoiceQueryService.findActiveInvoiceById(900L)).thenReturn(openInvoice(900L, 58000, 0));

        listener.onPaymentSettled(new PaymentSettledEvent(
            5L, "INVOICE", 900L, 58000, "INV-abc", LocalDateTime.now()
        ));

        verify(invoiceService).applyPayment(Long.valueOf(900L), Integer.valueOf(58000));
        verify(paymentApi).confirmSplit(
            Long.valueOf(5L), Integer.valueOf(58000), Integer.valueOf(0)
        );
    }

    @Test
    void overpay_parksExcessAndConfirmsSplit() {
        when(invoiceQueryService.findActiveInvoiceById(900L)).thenReturn(openInvoice(900L, 58000, 40000));

        listener.onPaymentSettled(new PaymentSettledEvent(
            5L, "INVOICE", 900L, 58000, "INV-abc", LocalDateTime.now()
        ));

        verify(invoiceService).applyPayment(Long.valueOf(900L), Integer.valueOf(18000));
        verify(paymentApi).confirmSplit(
            org.mockito.ArgumentMatchers.eq(5L),
            org.mockito.ArgumentMatchers.eq(18000),
            org.mockito.ArgumentMatchers.eq(40000)
        );
    }

    @Test
    void voidInvoice_parksEverythingWithoutApplying() {
        InvoiceResponse voided = new InvoiceResponse(
            900L, "INV-08092026-AAAAAA", null, InvoiceStatus.VOID,
            58000, 0, 58000,
            LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(),
            List.of()
        );
        when(invoiceQueryService.findActiveInvoiceById(900L)).thenReturn(voided);

        listener.onPaymentSettled(new PaymentSettledEvent(
            5L, "INVOICE", 900L, 58000, "INV-abc", LocalDateTime.now()
        ));

        verify(invoiceService, never()).applyPayment(anyLong(), anyInt());
        verify(paymentApi).confirmSplit(
            org.mockito.ArgumentMatchers.eq(5L),
            org.mockito.ArgumentMatchers.eq(0),
            org.mockito.ArgumentMatchers.eq(58000)
        );
    }

    @Test
    void legacyTarget_isIgnored() {
        listener.onPaymentSettled(new PaymentSettledEvent(
            5L, "ORDER", 101L, 58000, "INV-abc", LocalDateTime.now()
        ));

        verify(invoiceService, never()).applyPayment(anyLong(), anyInt());
        verify(invoiceQueryService, never()).findActiveInvoiceById(anyLong());
        verify(paymentApi, never()).confirmSplit(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    void replayedSettlement_isSkipped() {
        when(invoiceQueryService.findActiveInvoiceById(900L)).thenReturn(paidInvoice(900L, 58000));

        listener.onPaymentSettled(new PaymentSettledEvent(
            5L, "INVOICE", 900L, 58000, "INV-abc", LocalDateTime.now()
        ));

        verify(invoiceService, never()).applyPayment(anyLong(), anyInt());
        verify(paymentApi, never()).confirmSplit(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt()
        );
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
