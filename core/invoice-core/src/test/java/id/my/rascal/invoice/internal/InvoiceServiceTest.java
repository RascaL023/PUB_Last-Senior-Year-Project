package id.my.rascal.invoice.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.invoice.internal.entity.Invoice;
import id.my.rascal.invoice.internal.entity.InvoiceItem;
import id.my.rascal.invoice.internal.entity.InvoiceStatus;
import id.my.rascal.invoice.internal.model.request.ApplyPaymentRequest;
import id.my.rascal.invoice.internal.model.request.CreateInvoiceRequest;
import id.my.rascal.invoice.internal.model.request.InvoiceItemRequest;
import id.my.rascal.invoice.internal.model.response.InvoiceResponse;
import id.my.rascal.invoice.internal.repository.InvoiceRepository;
import id.my.rascal.invoice.internal.service.InvoiceService;

class InvoiceServiceTest {

    private InvoiceRepository invoiceRepository;
    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        when(invoiceRepository.existsByInvoiceNumber(any())).thenReturn(false);
        when(invoiceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        invoiceService = new InvoiceService(invoiceRepository);
    }

    @Test
    void create_persistsOpenInvoiceWithGeneratedNumber() {
        InvoiceResponse response = invoiceService.create(requestOf(225000));

        assertTrue(response.invoiceNumber().startsWith("INV-"));
        assertEquals(InvoiceStatus.OPEN, response.status());
        assertEquals(225000, response.totalAmount());
        assertEquals(0, response.paidAmount());
        assertEquals(225000, response.remainingAmount());
        assertEquals(1, response.items().size());
    }

    @Test
    void fullLifecycle_partialThenFull() {
        InvoiceResponse created = invoiceService.create(requestOf(225000));
        stubFindActive(created.id(), 225000, 0);

        InvoiceResponse partial = invoiceService.applyPayment(created.id(), new ApplyPaymentRequest(60000));
        assertEquals(60000, partial.paidAmount());
        assertEquals(165000, partial.remainingAmount());
        assertEquals(InvoiceStatus.PARTIALLY_PAID, partial.status());

        stubFindActive(created.id(), 225000, 60000);
        InvoiceResponse paid = invoiceService.applyPayment(created.id(), new ApplyPaymentRequest(165000));
        assertEquals(225000, paid.paidAmount());
        assertEquals(0, paid.remainingAmount());
        assertEquals(InvoiceStatus.PAID, paid.status());
    }

    @Test
    void overpayment_isRejected() {
        InvoiceResponse created = invoiceService.create(requestOf(225000));
        stubFindActive(created.id(), 225000, 0);

        assertThrows(BadRequestException.class,
            () -> invoiceService.applyPayment(created.id(), new ApplyPaymentRequest(225001)));
    }

    private void stubFindActive(Long id, int total, int paid) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setInvoiceNumber("INV-01012026-ABCDEF");
        invoice.setTotalAmount(total);
        invoice.setPaidAmount(paid);
        invoice.setRemainingAmount(total - paid);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setCreatedAt(LocalDateTime.now());
        if (paid == 0) invoice.markOpen();
        else if (paid == total) invoice.markPaid();
        else invoice.markPartiallyPaid();
        invoice.setItems(List.of(new InvoiceItem()));
        when(invoiceRepository.findActiveById(id)).thenReturn(Optional.of(invoice));
    }

    private CreateInvoiceRequest requestOf(int unitPrice) {
        return new CreateInvoiceRequest(List.of(
            new InvoiceItemRequest(1L, "Nasi Goreng", 1, unitPrice)
        ));
    }

}
