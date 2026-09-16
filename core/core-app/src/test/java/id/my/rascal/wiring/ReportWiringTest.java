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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import id.my.rascal.auth.internal.seeder.authority.AuthorityCatalog;
import id.my.rascal.common.exception.BadRequestException;
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
 *   <li>basis <b>tagihan</b> ({@code sales.billing}) terisi saat invoice lunas melalui
 *       {@code POST /payments} — satu-satunya jalur uang sejak B2 ditutup.</li>
 * </ul>
 *
 * <p>B9: lifecycle invoice (open → partial → lunas → label) sengaja berada di <b>satu</b>
 * test method dengan step berurutan, karena seluruh step membaca dashboard agregat yang sama.
 * Membuat fixture per test justru membuat assertion agregat ("1 invoice belum lunas")
 * saling mencemari antar test. Dengan penyatuan ini test bisa dijalankan sendirian.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    private InvoiceQueryService invoiceQueryService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private ReportController reportController;

    @BeforeEach
    void stubMenu() {
        when(menuApi.getMenuSnapshots(any())).thenAnswer(invocation -> menuSnapshots(invocation.getArgument(0), "Nasi Goreng"));
        when(menuApi.getModifierOptionSnapshots(any())).thenReturn(List.of());
    }

    @Test
    void reportEndpoint_isWiredWithItsAuthority() {
        assertNotNull(reportController);
        assertNotNull(reportService);
        assertTrue(AuthorityCatalog.names().contains("report.read"),
            "authority report.read harus terdaftar di catalog");
    }

    @Test
    void invoiceLifecycle_projectsToDashboard() {
        // ---- Step 1: invoice OPEN → tampil sebagai piutang, bukan pendapatan ----
        OrderResponse order = orderService.create(new OrderRequest(
            null, null, null, OrderType.TAKEAWAY,
            List.of(new OrderItemRequest(null, MENU_NASI_GORENG, 2, List.of()))
        ));
        Long orderId = order.id();

        InvoiceResponse invoice = awaitInvoiceForOrder(orderId);
        Long invoiceId = invoice.id();
        assertEquals(ORDER_AMOUNT, invoice.totalAmount());

        DashboardSummaryApiResponse summary = awaitSummary(
            s -> s.sales().billing().outstandingInvoices() == 1,
            "1 invoice belum lunas"
        );

        Billing billing = summary.sales().billing();
        assertEquals(0, billing.settledInvoices());
        assertEquals(0, billing.settledAmount());
        assertEquals(ORDER_AMOUNT, billing.outstandingAmount());
        // B7: piutang itu snapshot — wajib bawa penanda waktunya sendiri.
        assertNotNull(billing.outstandingAsOf(), "outstandingAsOf harus terisi");

        assertEquals(0, summary.sales().cash().received());

        assertTrue(summary.topMenus().isEmpty(), "invoice OPEN tidak masuk penjualan menu");
        assertEquals("OPEN", billingStatusOf(summary, orderId));

        // ---- Step 2 [B2]: partial pay via POST /payments — kas bergerak sebagian,
        // tagihan belum lunas → belum masuk settledAmount, tapi piutangnya berkurang. ----
        paymentService.create(new PaymentRequest(
            invoiceId, PaymentProvider.INTERNAL, null, MANUAL_PARTIAL_AMOUNT
        ));
        awaitInvoiceStatus(invoiceId, InvoiceStatus.PARTIALLY_PAID);

        summary = reportService.getDashboardSummary(null, null);

        Cash cash = summary.sales().cash();
        assertEquals(MANUAL_PARTIAL_AMOUNT, cash.received());

        billing = summary.sales().billing();
        assertEquals(0, billing.settledInvoices());
        assertEquals(0, billing.settledAmount());
        assertEquals(ORDER_AMOUNT - MANUAL_PARTIAL_AMOUNT, billing.outstandingAmount());
        assertNotNull(billing.outstandingAsOf(), "outstandingAsOf harus terisi");
        assertTrue(summary.topMenus().isEmpty(), "pembayaran parsial bukan penjualan lunas");
        assertEquals("PARTIALLY_PAID", billingStatusOf(summary, orderId));

        // ---- Step 3: pelunasan sisa (tanpa amount = remainingAmount) → invoice PAID ----
        PaymentResponse payment = paymentService.create(new PaymentRequest(
            invoiceId, PaymentProvider.INTERNAL, null, null
        ));
        assertEquals(CASH_PAYMENT_AMOUNT, payment.amount());

        awaitInvoiceStatus(invoiceId, InvoiceStatus.PAID);

        summary = awaitSummary(
            s -> s.sales().billing().settledInvoices() == 1 && !s.topMenus().isEmpty(),
            "invoice lunas terproyeksi ke dashboard"
        );

        cash = summary.sales().cash();
        assertEquals(ORDER_AMOUNT, cash.received());

        billing = summary.sales().billing();
        assertEquals(1, billing.settledInvoices());
        assertEquals(ORDER_AMOUNT, billing.settledAmount());
        assertEquals(ORDER_AMOUNT, billing.averageSettledInvoice());
        assertEquals(0, billing.outstandingInvoices());
        assertEquals(0, billing.outstandingAmount());

        // Dua basis kini bergerak bersama: satu-satunya jalur uang adalah payment (B2 ditutup),
        // jadi kas terakumulasi (partial 20.000 + pelunasan 30.000) == billing.settledAmount.
        assertEquals(billing.settledAmount(), cash.received());

        TopMenuEntry topMenu = summary.topMenus().get(0);
        assertEquals(MENU_NASI_GORENG, topMenu.menuId().longValue());
        assertEquals(2, topMenu.qty());
        // Penjualan menu harus bisa direkonsiliasi dengan settledAmount periode itu.
        assertEquals(billing.settledAmount(), topMenu.revenue());

        assertEquals("PAID", billingStatusOf(summary, orderId));

        // ---- Step 4: label top menu mengikuti master menu, bukan snapshot tagihan ----
        when(menuApi.getMenuSnapshots(any())).thenAnswer(invocation -> menuSnapshots(invocation.getArgument(0), "Nasi Goreng Spesial"));

        summary = awaitSummary(
            s -> !s.topMenus().isEmpty(),
            "menu sales masih ada"
        );

        topMenu = summary.topMenus().get(0);
        assertEquals(MENU_NASI_GORENG, topMenu.menuId().longValue());
        assertEquals("Nasi Goreng Spesial", topMenu.name(),
            "label diambil dari master menu, bukan snapshot deskripsi tagihan");
    }

    @Test
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
