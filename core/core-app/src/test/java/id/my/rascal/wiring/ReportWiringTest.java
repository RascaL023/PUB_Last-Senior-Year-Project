package id.my.rascal.wiring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import id.my.rascal.auth.internal.seeder.authority.AuthorityCatalog;
import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.invoice.internal.entity.InvoiceStatus;
import id.my.rascal.invoice.internal.model.response.InvoiceResponse;
import id.my.rascal.invoice.internal.service.InvoiceQueryService;
import id.my.rascal.menu.api.MenuApi;
import id.my.rascal.menu.api.MenuApiResponse;
import id.my.rascal.order.internal.model.enums.OrderType;
import id.my.rascal.order.internal.model.request.OrderItemRequest;
import id.my.rascal.order.internal.model.request.OrderRequest;
import id.my.rascal.order.internal.model.response.OrderResponse;
import id.my.rascal.order.internal.service.OrderService;
import id.my.rascal.payment.internal.model.enums.PaymentProvider;
import id.my.rascal.payment.internal.model.enums.PaymentTargetType;
import id.my.rascal.payment.internal.model.request.PaymentRequest;
import id.my.rascal.payment.internal.model.response.PaymentResponse;
import id.my.rascal.payment.internal.service.PaymentService;
import id.my.rascal.report.api.DashboardSummaryApiResponse;
import id.my.rascal.report.api.DashboardSummaryApiResponse.Billing;
import id.my.rascal.report.api.DashboardSummaryApiResponse.Cash;
import id.my.rascal.report.api.DashboardSummaryApiResponse.RecentActivityEntry;
import id.my.rascal.report.api.DashboardSummaryApiResponse.TopMenuEntry;
import id.my.rascal.report.internal.controller.ReportController;
import id.my.rascal.report.internal.service.ReportService;

/**
 * Verifikasi dashboard report sebagai pembaca langsung di atas domain invoice:
 *
 * <ul>
 *   <li>basis <b>kas</b> ({@code sales.cash}) hanya terisi dari payment yang benar-benar
 *       dialokasikan ke tagihan ({@code applied_amount});</li>
 *   <li>basis <b>tagihan</b> ({@code sales.billing}) terisi saat invoice lunas, termasuk
 *       pelunasan manual tanpa payment record;</li>
 *   <li>refund menambah fakta keluar dan TIDAK menulis ulang angka periode sebelumnya.</li>
 * </ul>
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class ReportWiringTest {

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        MenuApi menuApi() {
            return mock(MenuApi.class);
        }
    }

    private static final long MENU_NASI_GORENG = 10L;
    private static final int ORDER_AMOUNT = 50000;
    private static final int MANUAL_PARTIAL_AMOUNT = 20000;
    private static final int CASH_PAYMENT_AMOUNT = ORDER_AMOUNT - MANUAL_PARTIAL_AMOUNT;

    @Autowired
    private MenuApi menuApi;
    @Autowired
    private OrderService orderService;
    @Autowired
    private InvoiceApi invoiceApi;
    @Autowired
    private InvoiceQueryService invoiceQueryService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private ReportController reportController;

    private Long orderId;
    private Long invoiceId;
    private Long paymentId;

    @BeforeEach
    void stubMenu() {
        when(menuApi.getMenuSnapshots(any())).thenAnswer(invocation -> menuSnapshots(invocation.getArgument(0), "Nasi Goreng"));
        when(menuApi.getModifierOptionSnapshots(any())).thenReturn(List.of());
    }

    @Test
    @Order(1)
    void reportEndpoint_isWiredWithItsAuthority() {
        assertNotNull(reportController);
        assertNotNull(reportService);
        assertTrue(AuthorityCatalog.names().contains("report.read"),
            "authority report.read harus terdaftar di catalog");
    }

    @Test
    @Order(2)
    void openInvoice_showsAsOutstandingNotAsRevenue() {
        OrderResponse order = orderService.create(new OrderRequest(
            null, null, null, OrderType.TAKEAWAY,
            List.of(new OrderItemRequest(null, MENU_NASI_GORENG, 2, List.of()))
        ));
        orderId = order.id();

        InvoiceResponse invoice = awaitInvoiceForOrder(orderId);
        invoiceId = invoice.id();
        assertEquals(ORDER_AMOUNT, invoice.totalAmount());

        DashboardSummaryApiResponse summary = awaitSummary(
            s -> s.sales().billing().outstandingInvoices() == 1,
            "1 invoice belum lunas"
        );

        Billing billing = summary.sales().billing();
        assertEquals(0, billing.settledInvoices());
        assertEquals(0, billing.settledAmount());
        assertEquals(ORDER_AMOUNT, billing.outstandingAmount());

        assertEquals(0, summary.sales().cash().received());
        assertEquals(0, summary.sales().cash().refunded());
        assertEquals(0, summary.sales().cash().net());

        assertTrue(summary.topMenus().isEmpty(), "invoice OPEN tidak masuk penjualan menu");
        assertEquals("OPEN", billingStatusOf(summary, orderId));
    }

    @Test
    @Order(3)
    void manualInvoicePayment_movesToBillingOnlyNotToCash() {
        // Pelunasan manual (POST /invoices/{id}/payments) tidak membentuk Payment record.
        invoiceApi.applyPayment(invoiceId, MANUAL_PARTIAL_AMOUNT);
        awaitInvoiceStatus(invoiceId, InvoiceStatus.PARTIALLY_PAID);

        DashboardSummaryApiResponse summary = reportService.getDashboardSummary(null, null);

        // Kas tidak bergerak: tidak ada payment yang masuk.
        assertEquals(0, summary.sales().cash().received());
        // Tagihan belum lunas, jadi belum masuk settledAmount — tapi piutangnya berkurang.
        assertEquals(0, summary.sales().billing().settledInvoices());
        assertEquals(ORDER_AMOUNT - MANUAL_PARTIAL_AMOUNT, summary.sales().billing().outstandingAmount());
        assertTrue(summary.topMenus().isEmpty(), "pelunasan manual parsial bukan penjualan lunas");
        assertEquals("PARTIALLY_PAID", billingStatusOf(summary, orderId));
    }

    @Test
    @Order(4)
    void cashPayment_fillsCashAndBillingBases() {
        PaymentResponse payment = paymentService.create(new PaymentRequest(
            PaymentTargetType.INVOICE, invoiceId, PaymentProvider.INTERNAL, null
        ));
        paymentId = payment.id();
        assertEquals(CASH_PAYMENT_AMOUNT, payment.amount());

        awaitInvoiceStatus(invoiceId, InvoiceStatus.PAID);

        DashboardSummaryApiResponse summary = awaitSummary(
            s -> s.sales().billing().settledInvoices() == 1 && !s.topMenus().isEmpty(),
            "invoice lunas terproyeksi ke dashboard"
        );

        Cash cash = summary.sales().cash();
        assertEquals(CASH_PAYMENT_AMOUNT, cash.received());
        assertEquals(0, cash.refunded());
        assertEquals(CASH_PAYMENT_AMOUNT, cash.net());

        Billing billing = summary.sales().billing();
        assertEquals(1, billing.settledInvoices());
        assertEquals(ORDER_AMOUNT, billing.settledAmount());
        assertEquals(ORDER_AMOUNT, billing.averageSettledInvoice());
        assertEquals(0, billing.refundedAmount());
        assertEquals(0, billing.outstandingInvoices());
        assertEquals(0, billing.outstandingAmount());

        // Dua basis sengaja berbeda: 20.000 dari pelunasan manual tidak lewat payment,
        // jadi uang masuk (kas) lebih kecil dari nilai tagihan yang lunas.
        assertEquals(MANUAL_PARTIAL_AMOUNT, billing.settledAmount() - cash.received());

        TopMenuEntry topMenu = summary.topMenus().get(0);
        assertEquals(MENU_NASI_GORENG, topMenu.menuId().longValue());
        assertEquals("Nasi Goreng", topMenu.name());
        assertEquals(2, topMenu.qty());
        // Penjualan menu harus bisa direkonsiliasi dengan settledAmount periode itu.
        assertEquals(billing.settledAmount(), topMenu.revenue());

        assertEquals("PAID", billingStatusOf(summary, orderId));
    }

    @Test
    @Order(5)
    void refund_addsOutflowFactWithoutRewritingPastPeriod() {
        paymentService.markRefunded(paymentId);

        DashboardSummaryApiResponse summary = awaitSummary(
            s -> s.sales().cash().refunded() == CASH_PAYMENT_AMOUNT,
            "refund tercatat sebagai kas keluar"
        );

        Cash cash = summary.sales().cash();
        // Fakta periode lampau tidak berubah: uang masuk tetap tercatat 30.000.
        assertEquals(CASH_PAYMENT_AMOUNT, cash.received());
        assertEquals(CASH_PAYMENT_AMOUNT, cash.refunded());
        assertEquals(0, cash.net());

        Billing billing = summary.sales().billing();
        // Jurnal pelunasan tetap; koreksi tagihan muncul sebagai fakta refund sendiri.
        assertEquals(1, billing.settledInvoices());
        assertEquals(ORDER_AMOUNT, billing.settledAmount());
        assertEquals(ORDER_AMOUNT, billing.refundedAmount());
        // Invoice kembali OPEN dengan total 0 → tidak lagi dihitung sebagai piutang.
        assertEquals(0, billing.outstandingInvoices());
        assertEquals(0, billing.outstandingAmount());

        // Penjualan menu bruto tidak dikoreksi (refund sudah terlihat di cash/billing refunded).
        assertEquals(ORDER_AMOUNT, summary.topMenus().get(0).revenue());
    }

    @Test
    @Order(6)
    void topMenuLabel_followsMenuMasterNotInvoiceSnapshot() {
        when(menuApi.getMenuSnapshots(any())).thenAnswer(invocation -> menuSnapshots(invocation.getArgument(0), "Nasi Goreng Spesial"));

        DashboardSummaryApiResponse summary = awaitSummary(
            s -> !s.topMenus().isEmpty(),
            "menu sales masih ada setelah refund"
        );

        TopMenuEntry topMenu = summary.topMenus().get(0);
        assertEquals(MENU_NASI_GORENG, topMenu.menuId().longValue());
        assertEquals("Nasi Goreng Spesial", topMenu.name(),
            "label diambil dari master menu, bukan snapshot deskripsi tagihan");
    }

    @Test
    @Order(7)
    void invalidPeriod_isRejected() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate today = LocalDate.now();

        assertThrows(BadRequestException.class, () -> reportService.getDashboardSummary(tomorrow, today));
    }

    // ---- helpers ----

    private static List<MenuApiResponse> menuSnapshots(Collection<Long> ids, String name) {
        List<MenuApiResponse> result = new ArrayList<>();
        if (ids != null && ids.contains(MENU_NASI_GORENG))
            result.add(new MenuApiResponse(MENU_NASI_GORENG, name, 25000, true, List.of()));
        return result;
    }

    private String billingStatusOf(DashboardSummaryApiResponse summary, Long orderId) {
        return summary.recentActivity().stream()
            .filter(entry -> orderId.equals(entry.orderId()))
            .map(RecentActivityEntry::billingStatus)
            .findFirst()
            .orElseThrow(() -> new AssertionError("order " + orderId + " tidak ada di recentActivity"));
    }

    private InvoiceResponse awaitInvoiceForOrder(Long orderId) {
        return await("invoice untuk order " + orderId, () -> {
            Page<InvoiceResponse> page = invoiceQueryService.searchActive(
                null, null, null, orderId, PageRequest.of(0, 1)
            );
            return page.hasContent() ? page.getContent().get(0) : null;
        });
    }

    private InvoiceResponse awaitInvoiceStatus(Long invoiceId, InvoiceStatus status) {
        return await("invoice " + invoiceId + " berstatus " + status, () -> {
            InvoiceResponse invoice = invoiceQueryService.findActiveInvoiceById(invoiceId);
            return status == invoice.status() ? invoice : null;
        });
    }

    private DashboardSummaryApiResponse awaitSummary(
        Predicate<DashboardSummaryApiResponse> condition,
        String description
    ) {
        return await("dashboard: " + description, () -> {
            DashboardSummaryApiResponse summary = reportService.getDashboardSummary(null, null);
            return condition.test(summary) ? summary : null;
        });
    }

    private <T> T await(String description, Supplier<T> probe) {
        long deadline = System.currentTimeMillis() + 5000;
        while (true) {
            T result = probe.get();
            if (result != null) return result;
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
