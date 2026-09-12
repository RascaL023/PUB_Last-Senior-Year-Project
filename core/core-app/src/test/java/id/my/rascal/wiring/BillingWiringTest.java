package id.my.rascal.wiring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Pageable;

import id.my.rascal.dining.internal.model.request.CreateDiningOrderRequest;
import id.my.rascal.dining.internal.model.request.DiningOrderItemRequest;
import id.my.rascal.dining.internal.model.request.OpenDiningRequest;
import id.my.rascal.dining.internal.model.response.DiningResponse;
import id.my.rascal.dining.internal.service.DiningService;
import id.my.rascal.dining.internal.service.TableService;
import id.my.rascal.dining.internal.model.request.DiningTableRequest;
import id.my.rascal.invoice.api.InvoiceApiResponse;
import id.my.rascal.invoice.internal.model.mapper.InvoiceMapper;
import id.my.rascal.invoice.internal.model.response.InvoiceResponse;
import id.my.rascal.invoice.internal.service.InvoiceQueryService;
import id.my.rascal.menu.api.MenuApi;
import id.my.rascal.menu.api.MenuApiResponse;
import id.my.rascal.order.internal.model.enums.OrderType;
import id.my.rascal.order.internal.model.request.OrderItemRequest;
import id.my.rascal.order.internal.model.request.OrderRequest;
import id.my.rascal.order.internal.model.response.OrderResponse;
import id.my.rascal.order.internal.service.OrderQueryService;
import id.my.rascal.order.internal.service.OrderService;
import id.my.rascal.payment.api.PaymentApi;
import id.my.rascal.payment.api.PaymentApiWebhookRequest;
import id.my.rascal.payment.api.PaymentProcessorStatus;
import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.model.enums.PaymentProvider;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;
import id.my.rascal.payment.internal.model.enums.PaymentTargetType;
import id.my.rascal.payment.internal.model.request.PaymentRequest;
import id.my.rascal.payment.internal.repository.PaymentRepository;
import id.my.rascal.payment.internal.service.PaymentService;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class BillingWiringTest {

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        MenuApi menuApi() {
            return mock(MenuApi.class);
        }
    }

    @Autowired
    private MenuApi menuApi;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderQueryService orderQueryService;
    @Autowired
    private DiningService diningService;
    @Autowired
    private TableService tableService;
    @Autowired
    private InvoiceQueryService invoiceQueryService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentApi paymentApi;
    @Autowired
    private PaymentRepository paymentRepository;

    private Long diningId;
    private Long firstDiningOrderId;
    private Long secondDiningOrderId;
    private Long diningInvoiceId;

    @BeforeEach
    void stubMenu() {
        when(menuApi.getMenuSnapshots(List.of(10L))).thenReturn(List.of(
            new MenuApiResponse(10L, "Nasi Goreng", 25000, true, List.of())
        ));
        when(menuApi.getMenuSnapshots(List.of(11L))).thenReturn(List.of(
            new MenuApiResponse(11L, "Es Teh", 8000, true, List.of())
        ));
        when(menuApi.getModifierOptionSnapshots(any())).thenReturn(List.of());
    }

    @Test
    @Order(1)
    void standaloneOrder_createsInvoiceWithSnapshottedItems() {
        OrderResponse order = orderService.create(new OrderRequest(
            null, null, null, OrderType.TAKEAWAY,
            List.of(new OrderItemRequest(null, 10L, 2, List.of()))
        ));

        InvoiceApiResponse invoice = awaitInvoiceForOrder(order.id());

        assertEquals(1, invoice.items().size());
        assertEquals(2, invoice.items().get(0).quantity());
        assertEquals(25000, invoice.items().get(0).unitPrice());
        assertEquals(50000, invoice.items().get(0).amount());
        assertEquals(order.id(), invoice.items().get(0).orderId());
        assertEquals(50000, invoice.totalAmount());
        assertEquals("OPEN", invoice.status());
    }

    @Test
    @Order(2)
    void diningOrders_contributeToSingleInvoice() {
        Long tableId = tableService.create(new DiningTableRequest("W1")).id();
        DiningResponse dining = diningService.open(new OpenDiningRequest(tableId));
        diningId = dining.id();

        DiningResponse afterFirst = diningService.addOrder(diningId, diningOrderRequest());
        firstDiningOrderId = afterFirst.orders().get(0).id();

        DiningResponse afterSecond = diningService.addOrder(diningId, diningOrderRequest());
        secondDiningOrderId = afterSecond.orders().stream()
            .map(o -> o.id())
            .filter(id -> !id.equals(firstDiningOrderId))
            .findFirst()
            .orElseThrow();

        InvoiceApiResponse invoice = awaitDiningInvoice(diningId);
        diningInvoiceId = invoice.id();

        assertEquals(2, invoice.items().size());
        assertEquals(diningId, invoice.diningId());
        assertEquals(100000, invoice.totalAmount());
        assertTrue(invoice.items().stream()
            .map(i -> i.orderId())
            .toList()
            .containsAll(List.of(firstDiningOrderId, secondDiningOrderId)));
    }

    @Test
    @Order(3)
    void cancelledDiningOrder_removedFromInvoice() {
        orderService.cancel(secondDiningOrderId);

        InvoiceApiResponse invoice = awaitInvoiceWithItemCount(diningInvoiceId, 1);

        assertEquals(50000, invoice.totalAmount());
        assertEquals(firstDiningOrderId, invoice.items().get(0).orderId());
    }

    @Test
    @Order(4)
    void cashPayment_settlesInvoiceAndRecordsSplit() {
        paymentService.create(new PaymentRequest(PaymentTargetType.INVOICE, diningInvoiceId, PaymentProvider.INTERNAL, null));

        InvoiceApiResponse invoice = awaitInvoiceStatus(diningInvoiceId, "PAID");

        assertEquals(50000, invoice.paidAmount());
        assertEquals(0, invoice.remainingAmount());
    }

    @Test
    @Order(5)
    void webhookPaid_settlesStandaloneInvoice() {
        OrderResponse order = orderService.create(new OrderRequest(
            null, null, null, OrderType.TAKEAWAY,
            List.of(new OrderItemRequest(null, 11L, 1, List.of()))
        ));
        InvoiceApiResponse invoice = awaitInvoiceForOrder(order.id());

        Payment payment = new Payment();
        payment.setTargetType(PaymentTargetType.INVOICE);
        payment.setTargetId(invoice.id());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(8000);
        payment.setPaymentProvider(PaymentProvider.XENDIT);
        payment.setExternalId("TEST-WEBHOOK-1");
        payment.setCreatedAt(java.time.LocalDateTime.now());
        payment = paymentRepository.save(payment);

        paymentApi.handleWeebhookRequest(new PaymentApiWebhookRequest(
            "TEST-WEBHOOK-1", PaymentProcessorStatus.PAID, 8000, "QRIS", "XENDIT_QRIS", "IDR"
        ), "{}");

        InvoiceApiResponse settled = awaitInvoiceStatus(invoice.id(), "PAID");
        assertEquals(8000, settled.paidAmount());
    }

    private CreateDiningOrderRequest diningOrderRequest() {
        return new CreateDiningOrderRequest(
            null, "Budi", null,
            List.of(new DiningOrderItemRequest(10L, 2, List.of()))
        );
    }

    private InvoiceApiResponse awaitInvoiceForOrder(Long orderId) {
        AtomicReference<InvoiceApiResponse> found = new AtomicReference<>();
        await("invoice for order " + orderId, () -> {
            List<InvoiceResponse> matches = invoiceQueryService
                .searchActive(null, null, null, orderId, Pageable.ofSize(10))
                .getContent();
            if (!matches.isEmpty()) found.set(InvoiceMapper.toApiResponse(matches.get(0)));
            return !matches.isEmpty();
        });
        return found.get();
    }

    private InvoiceApiResponse awaitDiningInvoice(Long diningId) {
        AtomicReference<InvoiceApiResponse> found = new AtomicReference<>();
        await("dining invoice " + diningId, () -> {
            InvoiceApiResponse invoice = invoiceQueryService.findActiveInvoiceByDiningId(diningId);
            found.set(invoice);
            return invoice != null;
        });
        return found.get();
    }

    private InvoiceApiResponse awaitInvoiceWithItemCount(Long invoiceId, int items) {
        AtomicReference<InvoiceApiResponse> found = new AtomicReference<>();
        await("invoice " + invoiceId + " with " + items + " items", () -> {
            InvoiceResponse invoice = invoiceQueryService.findActiveInvoiceById(invoiceId);
            if (invoice.items().size() == items) {
                found.set(InvoiceMapper.toApiResponse(invoice));
                return true;
            }
            return false;
        });
        return found.get();
    }

    private InvoiceApiResponse awaitInvoiceStatus(Long invoiceId, String status) {
        AtomicReference<InvoiceApiResponse> found = new AtomicReference<>();
        await("invoice " + invoiceId + " status " + status, () -> {
            InvoiceResponse invoice = invoiceQueryService.findActiveInvoiceById(invoiceId);
            if (status.equals(invoice.status().name())) {
                found.set(InvoiceMapper.toApiResponse(invoice));
                return true;
            }
            return false;
        });
        return found.get();
    }

    private void await(String description, java.util.function.BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + 5000;
        while (true) {
            if (condition.getAsBoolean()) return;
            if (System.currentTimeMillis() > deadline)
                throw new AssertionError("Timed out waiting: " + description);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted waiting: " + description);
            }
        }
    }

}
