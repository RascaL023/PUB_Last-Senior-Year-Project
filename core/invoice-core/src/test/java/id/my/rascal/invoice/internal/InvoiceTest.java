package id.my.rascal.invoice.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.invoice.internal.entity.Invoice;
import id.my.rascal.invoice.internal.entity.InvoiceItem;
import id.my.rascal.invoice.internal.entity.InvoiceStatus;

class InvoiceTest {

    @Test
    void create_initializesOpenWithZeroPaid() {
        Invoice invoice = newInvoice(225000);

        assertEquals(InvoiceStatus.OPEN, invoice.getStatus());
        assertEquals(0, invoice.getPaidAmount());
        assertEquals(225000, invoice.getRemainingAmount());
    }

    @Test
    void applyPartialPayment_updatesPaidRemainingAndStatus() {
        Invoice invoice = newInvoice(225000);

        invoice.applyPayment(60000);

        assertEquals(60000, invoice.getPaidAmount());
        assertEquals(165000, invoice.getRemainingAmount());
        assertEquals(InvoiceStatus.PARTIALLY_PAID, invoice.getStatus());
    }

    @Test
    void applyFullPayment_marksPaid() {
        Invoice invoice = newInvoice(225000);

        invoice.applyPayment(225000);

        assertEquals(225000, invoice.getPaidAmount());
        assertEquals(0, invoice.getRemainingAmount());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
    }

    @Test
    void applyPaymentExceedingRemaining_isRejected() {
        Invoice invoice = newInvoice(225000);

        assertThrows(BadRequestException.class, () -> invoice.applyPayment(225001));
    }

    @Test
    void applyPaymentWithZeroOrNegative_isRejected() {
        Invoice invoice = newInvoice(225000);

        assertThrows(BadRequestException.class, () -> invoice.applyPayment(0));
        assertThrows(BadRequestException.class, () -> invoice.applyPayment(-1000));
    }

    @Test
    void voidInvoice_cannotReceivePayment() {
        Invoice invoice = newInvoice(225000);

        invoice.voidInvoice();

        assertEquals(InvoiceStatus.VOID, invoice.getStatus());
        assertThrows(BadRequestException.class, () -> invoice.applyPayment(10000));
    }

    @Test
    void voidPaidInvoice_isRejected() {
        Invoice invoice = newInvoice(225000);
        invoice.applyPayment(225000);

        assertThrows(BadRequestException.class, invoice::voidInvoice);
    }

    private Invoice newInvoice(int total) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-01012026-ABCDEF");
        invoice.setTotalAmount(total);
        invoice.setPaidAmount(0);
        invoice.setRemainingAmount(total);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.markOpen();
        invoice.setItems(new ArrayList<InvoiceItem>());
        return invoice;
    }

}
