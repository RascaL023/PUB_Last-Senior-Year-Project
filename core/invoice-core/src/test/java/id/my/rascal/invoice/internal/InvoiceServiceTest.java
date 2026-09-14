package id.my.rascal.invoice.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.dining.api.event.DiningOrderAddedEvent;
import id.my.rascal.invoice.internal.entity.Invoice;
import id.my.rascal.invoice.internal.entity.InvoiceItem;
import id.my.rascal.invoice.internal.entity.InvoiceStatus;
import id.my.rascal.invoice.internal.model.request.ApplyPaymentRequest;
import id.my.rascal.invoice.internal.model.request.CreateInvoiceRequest;
import id.my.rascal.invoice.internal.model.request.InvoiceItemRequest;
import id.my.rascal.invoice.internal.model.response.InvoiceResponse;
import id.my.rascal.invoice.internal.repository.InvoiceRepository;
import id.my.rascal.invoice.internal.service.InvoiceEventPublisherService;
import id.my.rascal.invoice.internal.service.InvoiceService;
import id.my.rascal.order.api.OrderTypeApiResponse;
import id.my.rascal.order.api.event.OrderCancelledEvent;
import id.my.rascal.order.api.event.OrderItemsChangedEvent;
import id.my.rascal.order.api.event.StandaloneOrderCreatedEvent;
import id.my.rascal.order.api.event.dto.OrderItemSnapshot;

class InvoiceServiceTest {

    private InvoiceRepository invoiceRepository;
    private InvoiceEventPublisherService invoiceEventPublisherService;
    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        invoiceEventPublisherService = mock(InvoiceEventPublisherService.class);
        when(invoiceRepository.existsByInvoiceNumber(any())).thenReturn(false);
        when(invoiceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        invoiceService = new InvoiceService(invoiceRepository, mock(id.my.rascal.invoice.internal.repository.RefundRepository.class), invoiceEventPublisherService);
    }

    @Test
    void create_persistsOpenInvoiceWithGeneratedNumber() {
        InvoiceResponse response = invoiceService.create(requestOf(null, 101L, 225000));

        assertTrue(response.invoiceNumber().startsWith("INV-"));
        assertEquals(InvoiceStatus.OPEN, response.status());
        assertEquals(225000, response.totalAmount());
        assertEquals(0, response.paidAmount());
        assertEquals(225000, response.remainingAmount());
        assertEquals(1, response.items().size());
    }

    @Test
    void fullLifecycle_partialThenFull() {
        InvoiceResponse created = invoiceService.create(requestOf(null, 101L, 225000));
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
        InvoiceResponse created = invoiceService.create(requestOf(null, 101L, 225000));
        stubFindActive(created.id(), 225000, 0);

        assertThrows(BadRequestException.class,
            () -> invoiceService.applyPayment(created.id(), new ApplyPaymentRequest(225001)));
    }

    @Test
    void applyPayment_publishesPaidEventPerApplication() {
        InvoiceResponse created = invoiceService.create(requestOf(null, 101L, 225000));
        verify(invoiceEventPublisherService).publishCreated(any(Invoice.class));
        stubFindActive(created.id(), 225000, 0);

        invoiceService.applyPayment(created.id(), new ApplyPaymentRequest(60000));

        ArgumentCaptor<Invoice> paid = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceEventPublisherService).publishPaid(paid.capture());
        assertEquals(60000, paid.getValue().getPaidAmount());
        assertEquals(165000, paid.getValue().getRemainingAmount());
    }

    @Test
    void standaloneOrderCreated_createsOneItemPerOrderItem() {
        StandaloneOrderCreatedEvent event = new StandaloneOrderCreatedEvent(
            101L, "ORD-01012026-AAAAAA", 7L, "John Doe", OrderTypeApiResponse.TAKEAWAY, 58000,
            List.of(
                new OrderItemSnapshot(1L, 10L, "Nasi Goreng", 2, 25000, 50000),
                new OrderItemSnapshot(2L, 11L, "Es Teh", 1, 8000, 8000)
            ),
            LocalDateTime.now()
        );

        InvoiceResponse response = invoiceService.handleStandaloneOrderCreated(event);

        assertNotNull(response);
        assertEquals(InvoiceStatus.OPEN, response.status());
        assertEquals(2, response.items().size());
        assertEquals(1L, response.items().get(0).orderItemId());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(25000, response.items().get(0).unitPrice());
        assertEquals(50000, response.items().get(0).amount());
        assertEquals(58000, response.totalAmount());
        assertEquals(58000, response.remainingAmount());
    }

    @Test
    void standaloneOrderCreated_duplicateEventCreatesNothing() {
        when(invoiceRepository.existsByItemsOrderItemId(1L)).thenReturn(true);
        StandaloneOrderCreatedEvent event = new StandaloneOrderCreatedEvent(
            101L, "ORD-01012026-AAAAAA", null, null, OrderTypeApiResponse.TAKEAWAY, 50000,
            List.of(new OrderItemSnapshot(1L, 10L, "Nasi Goreng", 2, 25000, 50000)),
            LocalDateTime.now()
        );

        assertNull(invoiceService.handleStandaloneOrderCreated(event));
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void diningOrders_contributeToSingleInvoice() {
        when(invoiceRepository.findActiveByDiningId(20L)).thenReturn(Optional.empty());

        InvoiceResponse first = invoiceService.handleOrderAddedToDining(new DiningOrderAddedEvent(
            20L, 101L, "ORD-01012026-AAAAAA", 50000,
            List.of(new OrderItemSnapshot(1L, 10L, "Nasi Goreng", 2, 25000, 50000)),
            LocalDateTime.now()
        ));
        assertEquals(50000, first.totalAmount());
        assertEquals(1, first.items().size());

        ArgumentCaptor<Invoice> saved = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(saved.capture());
        Invoice diningInvoice = saved.getValue();
        diningInvoice.setId(900L);
        when(invoiceRepository.findActiveByDiningId(20L)).thenReturn(Optional.of(diningInvoice));

        InvoiceResponse second = invoiceService.handleOrderAddedToDining(new DiningOrderAddedEvent(
            20L, 102L, "ORD-01012026-BBBBBB", 30000,
            List.of(new OrderItemSnapshot(3L, 12L, "Kopi", 2, 15000, 30000)),
            LocalDateTime.now()
        ));
        assertEquals(80000, second.totalAmount());
        assertEquals(2, second.items().size());
    }

    @Test
    void diningOrderAdded_duplicateEventAddsNothing() {
        Invoice diningInvoice = persistedDiningInvoice(20L, 101L, 1L, 50000);
        when(invoiceRepository.findActiveByDiningId(20L)).thenReturn(Optional.of(diningInvoice));

        InvoiceResponse response = invoiceService.handleOrderAddedToDining(new DiningOrderAddedEvent(
            20L, 101L, "ORD-01012026-AAAAAA", 50000,
            List.of(new OrderItemSnapshot(1L, 10L, "Nasi Goreng", 2, 25000, 50000)),
            LocalDateTime.now()
        ));

        assertEquals(50000, response.totalAmount());
        assertEquals(1, response.items().size());
    }

    @Test
    void orderCancelled_voidsUnpaidStandaloneInvoice() {
        Invoice invoice = persistedDiningInvoice(null, 101L, 1L, 50000);
        when(invoiceRepository.findActiveByItemsOrderId(101L)).thenReturn(List.of(invoice));

        invoiceService.handleOrderCancelled(new OrderCancelledEvent(101L));

        verify(invoiceRepository).save(invoice);
        assertEquals(InvoiceStatus.VOID, invoice.getStatus());
    }

    @Test
    void diningOrderCancelled_removesLinesAndRecomputes() {
        Invoice invoice = persistedDiningInvoice(20L, 101L, 1L, 50000);
        invoice.getItems().add(itemOf(invoice, 102L, 3L, "Kopi", 2, 15000, 30000));
        invoice.setTotalAmount(80000);
        invoice.setRemainingAmount(80000);
        when(invoiceRepository.findActiveByItemsOrderId(102L)).thenReturn(List.of(invoice));

        invoiceService.handleOrderCancelled(new OrderCancelledEvent(102L));

        verify(invoiceRepository).save(invoice);
        assertEquals(1, invoice.getItems().size());
        assertEquals(101L, invoice.getItems().get(0).getOrderId());
        assertEquals(50000, invoice.getTotalAmount());
        assertEquals(50000, invoice.getRemainingAmount());
        assertEquals(InvoiceStatus.OPEN, invoice.getStatus());
    }

    @Test
    void diningOrderCancelled_lastOrderVoidsInvoice() {
        Invoice invoice = persistedDiningInvoice(20L, 101L, 1L, 50000);
        when(invoiceRepository.findActiveByItemsOrderId(101L)).thenReturn(List.of(invoice));

        invoiceService.handleOrderCancelled(new OrderCancelledEvent(101L));

        verify(invoiceRepository).save(invoice);
        assertTrue(invoice.getItems().isEmpty());
        assertEquals(0, invoice.getTotalAmount());
        assertEquals(InvoiceStatus.VOID, invoice.getStatus());
    }

    @Test
    void diningOrderCancelled_paidInvoiceLeftForManual() {
        Invoice invoice = persistedDiningInvoice(20L, 101L, 1L, 50000);
        invoice.setPaidAmount(50000);
        invoice.setRemainingAmount(0);
        invoice.markPaid();
        when(invoiceRepository.findActiveByItemsOrderId(101L)).thenReturn(List.of(invoice));

        invoiceService.handleOrderCancelled(new OrderCancelledEvent(101L));

        verify(invoiceRepository, never()).save(any());
        assertEquals(1, invoice.getItems().size());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
    }

    @Test
    void itemsChanged_standaloneOpenInvoice_reconcilesLines() {
        Invoice invoice = persistedDiningInvoice(null, 101L, 1L, 50000);
        invoice.getItems().add(itemOf(invoice, 101L, 2L, "Es Teh", 1, 8000, 8000));
        invoice.setTotalAmount(58000);
        invoice.setRemainingAmount(58000);
        when(invoiceRepository.findActiveByItemsOrderId(101L)).thenReturn(List.of(invoice));

        invoiceService.handleOrderItemsChanged(new OrderItemsChangedEvent(
            101L, "ORD-1",
            List.of(
                new OrderItemSnapshot(1L, 10L, "Nasi Goreng", 3, 25000, 75000),
                new OrderItemSnapshot(3L, 12L, "Kopi", 1, 15000, 15000)
            ),
            LocalDateTime.now()
        ));

        verify(invoiceRepository).save(invoice);
        assertEquals(2, invoice.getItems().size());
        assertEquals(3, invoice.getItems().stream().filter(i -> i.getOrderItemId().equals(1L)).findFirst().get().getQuantity());
        assertTrue(invoice.getItems().stream().anyMatch(i -> i.getOrderItemId().equals(3L)));
        assertTrue(invoice.getItems().stream().noneMatch(i -> i.getOrderItemId().equals(2L)));
        assertEquals(90000, invoice.getTotalAmount());
        assertEquals(90000, invoice.getRemainingAmount());
    }

    @Test
    void itemsChanged_diningInvoice_onlyTouchesOwnOrderLines() {
        Invoice invoice = persistedDiningInvoice(20L, 101L, 1L, 50000);
        invoice.getItems().add(itemOf(invoice, 102L, 3L, "Kopi", 2, 15000, 30000));
        invoice.setTotalAmount(80000);
        invoice.setRemainingAmount(80000);
        when(invoiceRepository.findActiveByItemsOrderId(101L)).thenReturn(List.of(invoice));

        invoiceService.handleOrderItemsChanged(new OrderItemsChangedEvent(
            101L, "ORD-1",
            List.of(new OrderItemSnapshot(1L, 10L, "Nasi Goreng", 1, 25000, 25000)),
            LocalDateTime.now()
        ));

        assertEquals(2, invoice.getItems().size());
        assertEquals(25000, invoice.getItems().stream()
            .filter(i -> i.getOrderId().equals(101L)).findFirst().get().getAmount());
        assertEquals(30000, invoice.getItems().stream()
            .filter(i -> i.getOrderId().equals(102L)).findFirst().get().getAmount());
        assertEquals(55000, invoice.getTotalAmount());
    }

    @Test
    void itemsChanged_paidInvoice_skippedWithErrorLog() {
        Invoice invoice = persistedDiningInvoice(null, 101L, 1L, 50000);
        invoice.setPaidAmount(10000);
        invoice.setRemainingAmount(40000);
        invoice.markPartiallyPaid();
        when(invoiceRepository.findActiveByItemsOrderId(101L)).thenReturn(List.of(invoice));

        invoiceService.handleOrderItemsChanged(new OrderItemsChangedEvent(
            101L, "ORD-1",
            List.of(new OrderItemSnapshot(1L, 10L, "Nasi Goreng", 5, 25000, 125000)),
            LocalDateTime.now()
        ));

        verify(invoiceRepository, never()).save(any());
        assertEquals(1, invoice.getItems().size());
        assertEquals(50000, invoice.getTotalAmount());
    }

    @Test
    void itemsChanged_emptiedDiningInvoice_voided() {
        Invoice invoice = persistedDiningInvoice(20L, 101L, 1L, 50000);
        when(invoiceRepository.findActiveByItemsOrderId(101L)).thenReturn(List.of(invoice));

        invoiceService.handleOrderItemsChanged(new OrderItemsChangedEvent(
            101L, "ORD-1", List.of(), LocalDateTime.now()
        ));

        verify(invoiceRepository).save(invoice);
        assertTrue(invoice.getItems().isEmpty());
        assertEquals(InvoiceStatus.VOID, invoice.getStatus());
    }

    @Test
    void itemsChanged_replay_isIdempotent() {
        Invoice invoice = persistedDiningInvoice(null, 101L, 1L, 50000);
        when(invoiceRepository.findActiveByItemsOrderId(101L)).thenReturn(List.of(invoice));
        OrderItemsChangedEvent event = new OrderItemsChangedEvent(
            101L, "ORD-1",
            List.of(
                new OrderItemSnapshot(1L, 10L, "Nasi Goreng", 2, 25000, 50000),
                new OrderItemSnapshot(2L, 11L, "Es Teh", 1, 8000, 8000)
            ),
            LocalDateTime.now()
        );

        invoiceService.handleOrderItemsChanged(event);
        invoiceService.handleOrderItemsChanged(event);

        assertEquals(2, invoice.getItems().size());
        assertEquals(58000, invoice.getTotalAmount());
        assertEquals(58000, invoice.getRemainingAmount());
    }

    @Test
    void diningOrderAdded_toFinalInvoice_rejectedWithoutSave() {
        Invoice invoice = persistedDiningInvoice(20L, 101L, 1L, 50000);
        invoice.setPaidAmount(50000);
        invoice.setRemainingAmount(0);
        invoice.markPaid();
        when(invoiceRepository.findActiveByDiningId(20L)).thenReturn(Optional.of(invoice));

        invoiceService.handleOrderAddedToDining(new DiningOrderAddedEvent(
            20L, 102L, "ORD-08092026-BBBBBB", 30000,
            List.of(new OrderItemSnapshot(3L, 12L, "Kopi", 2, 15000, 30000)),
            LocalDateTime.now()
        ));

        verify(invoiceRepository, never()).save(any());
        assertEquals(1, invoice.getItems().size());
        assertEquals(50000, invoice.getTotalAmount());
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
        invoice.setItems(new ArrayList<>());
        when(invoiceRepository.findActiveById(id)).thenReturn(Optional.of(invoice));
    }

    @Test
    void refundItems_partiallyPaid_adjustsTotalAndCash() {
        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("INV-01012026-ABCDEF");
        invoice.setTotalAmount(115000);
        invoice.setPaidAmount(100000);
        invoice.setRemainingAmount(15000);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.markPartiallyPaid();
        invoice.setItems(new ArrayList<>(List.of(
            itemOf(invoice, 1L, 1L, "Item1", 1, 25000, 25000),
            itemOf(invoice, 1L, 2L, "Item2", 1, 30000, 30000),
            itemOf(invoice, 1L, 3L, "Item3", 1, 60000, 60000)
        )));
        when(invoiceRepository.findActiveById(1L)).thenReturn(Optional.of(invoice));

        InvoiceResponse res = invoiceService.refundItems(1L, new id.my.rascal.invoice.internal.model.request.RefundRequest(null, List.of(2L), null));

        assertEquals(85000, res.totalAmount());
        assertEquals(85000, res.paidAmount());
        assertEquals(0, res.remainingAmount());
        assertEquals(InvoiceStatus.PAID, res.status());
        assertTrue(res.items().stream().filter(i -> i.orderItemId().equals(2L)).findFirst().get().refunded());
    }

    @Test
    void refundItems_unpaid_adjustsTotalOnly() {
        Invoice invoice = new Invoice();
        invoice.setId(2L);
        invoice.setInvoiceNumber("INV-01012026-ABCDEF2");
        invoice.setTotalAmount(80000);
        invoice.setPaidAmount(0);
        invoice.setRemainingAmount(80000);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.markOpen();
        invoice.setItems(new ArrayList<>(List.of(
            itemOf(invoice, 1L, 10L, "A", 1, 50000, 50000),
            itemOf(invoice, 1L, 11L, "B", 1, 20000, 20000),
            itemOf(invoice, 1L, 12L, "C", 1, 10000, 10000)
        )));
        when(invoiceRepository.findActiveById(2L)).thenReturn(Optional.of(invoice));

        InvoiceResponse res = invoiceService.refundItems(2L, new id.my.rascal.invoice.internal.model.request.RefundRequest(null, List.of(12L), null));

        assertEquals(70000, res.totalAmount());
        assertEquals(0, res.paidAmount());
        assertEquals(70000, res.remainingAmount());
        assertEquals(InvoiceStatus.OPEN, res.status());
    }

    @Test
    void refundItems_alreadyRefunded_rejected() {
        Invoice invoice = persistedDiningInvoice(null, 1L, 1L, 50000);
        invoice.getItems().get(0).setRefunded(true);
        when(invoiceRepository.findActiveById(900L)).thenReturn(Optional.of(invoice));

        assertThrows(BadRequestException.class,
            () -> invoiceService.refundItems(900L, new id.my.rascal.invoice.internal.model.request.RefundRequest(null, List.of(1L), null)));
    }

    private Invoice persistedDiningInvoice(Long diningId, Long orderId, Long orderItemId, int amount) {
        Invoice invoice = new Invoice();
        invoice.setId(900L);
        invoice.setInvoiceNumber("INV-01012026-ABCDEF");
        invoice.setDiningId(diningId);
        invoice.setTotalAmount(amount);
        invoice.setPaidAmount(0);
        invoice.setRemainingAmount(amount);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.markOpen();

        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setOrderItemId(orderItemId);
        item.setOrderId(orderId);
        item.setDescription("Nasi Goreng");
        item.setQuantity(2);
        item.setUnitPrice(25000);
        item.setAmount(amount);
        item.setCreatedAt(LocalDateTime.now());
        invoice.setItems(new ArrayList<>(List.of(item)));
        return invoice;
    }

    private InvoiceItem itemOf(Invoice invoice, Long orderId, Long orderItemId,
                               String description, int quantity, int unitPrice, int amount) {
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setOrderItemId(orderItemId);
        item.setOrderId(orderId);
        item.setDescription(description);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setAmount(amount);
        item.setCreatedAt(LocalDateTime.now());
        return item;
    }

    private CreateInvoiceRequest requestOf(Long diningId, Long orderId, int amount) {
        return new CreateInvoiceRequest(diningId, List.of(
            new InvoiceItemRequest(1L, orderId, "Nasi Goreng", 1, amount, amount)
        ));
    }

}
