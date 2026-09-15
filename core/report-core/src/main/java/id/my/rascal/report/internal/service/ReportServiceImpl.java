package id.my.rascal.report.internal.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.dining.api.DiningReportApi;
import id.my.rascal.invoice.api.InvoiceReportApi;
import id.my.rascal.invoice.api.InvoiceReportApi.BillingMetrics;
import id.my.rascal.menu.api.MenuApi;
import id.my.rascal.menu.api.MenuApiResponse;
import id.my.rascal.order.api.OrderReportApi;
import id.my.rascal.payment.api.PaymentReportApi;
import id.my.rascal.report.api.DashboardSummaryApiResponse;
import id.my.rascal.report.api.DashboardSummaryApiResponse.Billing;
import id.my.rascal.report.api.DashboardSummaryApiResponse.Cash;
import id.my.rascal.report.api.DashboardSummaryApiResponse.Operations;
import id.my.rascal.report.api.DashboardSummaryApiResponse.Period;
import id.my.rascal.report.api.DashboardSummaryApiResponse.RecentActivityEntry;
import id.my.rascal.report.api.DashboardSummaryApiResponse.Sales;
import id.my.rascal.report.api.DashboardSummaryApiResponse.TopMenuEntry;

/**
 * Report adalah <b>pembaca langsung</b>: tidak ada tabel projection dan tidak ada listener event.
 * Semua angka diambil saat request dari fakta yang sudah dimiliki domain lain
 * (invoice = tagihan, payment = kas, order/dining = operasional) lewat contract.
 */
@Service
public class ReportServiceImpl implements ReportService {

    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");
    private static final int TOP_MENUS_LIMIT = 10;
    private static final int RECENT_ACTIVITY_LIMIT = 10;

    private final InvoiceReportApi invoiceReportApi;
    private final PaymentReportApi paymentReportApi;
    private final OrderReportApi orderReportApi;
    private final DiningReportApi diningReportApi;
    private final MenuApi menuApi;

    public ReportServiceImpl(
        InvoiceReportApi invoiceReportApi,
        PaymentReportApi paymentReportApi,
        OrderReportApi orderReportApi,
        DiningReportApi diningReportApi,
        MenuApi menuApi
    ) {
        this.invoiceReportApi = invoiceReportApi;
        this.paymentReportApi = paymentReportApi;
        this.orderReportApi = orderReportApi;
        this.diningReportApi = diningReportApi;
        this.menuApi = menuApi;
    }

    @Override
    public DashboardSummaryApiResponse getDashboardSummary(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(JAKARTA);
        LocalDate fromDate = from == null ? today : from;
        LocalDate toDate = to == null ? today : to;

        if (toDate.isBefore(fromDate))
            throw new BadRequestException("'to' date must not be before 'from' date");

        LocalDateTime start = startOfDayInServerZone(fromDate);
        LocalDateTime end = startOfDayInServerZone(toDate.plusDays(1));

        BillingMetrics billing = invoiceReportApi.billingMetrics(start, end);
        long cashReceived = paymentReportApi.sumAppliedSettledBetween(start, end);
        long cashRefunded = paymentReportApi.sumRefundedBetween(start, end);

        Sales sales = new Sales(
            new Cash(cashReceived, cashRefunded, cashReceived - cashRefunded),
            new Billing(
                billing.settledInvoices(),
                billing.settledAmount(),
                billing.averageSettledInvoice(),
                billing.refundedAmount(),
                billing.outstandingInvoices(),
                billing.outstandingAmount()
            )
        );

        Operations operations = new Operations(
            diningReportApi.countOpenDinings(),
            diningReportApi.countOccupiedTables(),
            diningReportApi.countAvailableTables(),
            orderReportApi.countOrdersInProgress()
        );

        List<TopMenuEntry> topMenus = toTopMenus(
            invoiceReportApi.topMenuSales(start, end, TOP_MENUS_LIMIT)
        );

        List<RecentActivityEntry> recentActivity = withBillingStatus(
            orderReportApi.recentActivity(RECENT_ACTIVITY_LIMIT)
        );

        return new DashboardSummaryApiResponse(
            new Period(fromDate, toDate),
            sales,
            operations,
            topMenus,
            recentActivity
        );
    }

    /**
     * Label menu diambil dari master menu (bukan snapshot deskripsi tagihan) supaya nama yang
     * tampil adalah nama yang sekarang dikenal; deskripsi tagihan dipakai sebagai fallback bila
     * menu sudah tidak ada / baris tagihan manual.
     */
    private List<TopMenuEntry> toTopMenus(List<InvoiceReportApi.MenuSalesRow> rows) {
        if (rows.isEmpty()) return List.of();

        List<Long> menuIds = rows.stream()
            .map(InvoiceReportApi.MenuSalesRow::menuId)
            .filter(java.util.Objects::nonNull)
            .toList();

        Map<Long, String> namesByMenuId = menuIds.isEmpty()
            ? Map.of()
            : menuApi.getMenuSnapshots(menuIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                    MenuApiResponse::id,
                    MenuApiResponse::name,
                    (first, second) -> first
                ));

        return rows.stream()
            .map(row -> new TopMenuEntry(
                row.menuId(),
                namesByMenuId.getOrDefault(row.menuId(), row.description()),
                row.qty(),
                row.revenue()
            ))
            .toList();
    }

    /**
     * Order tidak menyimpan status bayar — status billing tiap order diambil dari invoice aktif
     * lewat satu query batch (hindari N+1).
     */
    private List<RecentActivityEntry> withBillingStatus(List<OrderReportApi.RecentActivityEntry> orders) {
        if (orders.isEmpty()) return List.of();

        Map<Long, String> billingStatusByOrderId = invoiceReportApi.billingStatusByOrderIds(
            orders.stream().map(OrderReportApi.RecentActivityEntry::orderId).toList()
        );

        return orders.stream()
            .map(order -> new RecentActivityEntry(
                order.orderId(),
                order.orderNumber(),
                order.status(),
                billingStatusByOrderId.get(order.orderId()),
                order.totalPrice(),
                order.createdAt()
            ))
            .toList();
    }

    private LocalDateTime startOfDayInServerZone(LocalDate date) {
        return date.atStartOfDay()
            .atZone(JAKARTA)
            .withZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime();
    }

}
