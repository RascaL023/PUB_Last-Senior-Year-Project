package id.my.rascal.report.internal.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.dining.api.DiningReportApi;
import id.my.rascal.order.api.OrderReportApi;
import id.my.rascal.payment.api.PaymentReportApi;
import id.my.rascal.report.api.DashboardSummaryApiResponse;
import id.my.rascal.report.api.DashboardSummaryApiResponse.Operations;
import id.my.rascal.report.api.DashboardSummaryApiResponse.Period;
import id.my.rascal.report.api.DashboardSummaryApiResponse.RecentActivityEntry;
import id.my.rascal.report.api.DashboardSummaryApiResponse.Sales;
import id.my.rascal.report.api.DashboardSummaryApiResponse.TopMenuEntry;

@Service
public class ReportServiceImpl implements ReportService {

    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");
    private static final int TOP_MENUS_LIMIT = 10;
    private static final int RECENT_ACTIVITY_LIMIT = 10;

    private final OrderReportApi orderReportApi;
    private final PaymentReportApi paymentReportApi;
    private final DiningReportApi diningReportApi;

    public ReportServiceImpl(
        OrderReportApi orderReportApi,
        PaymentReportApi paymentReportApi,
        DiningReportApi diningReportApi
    ) {
        this.orderReportApi = orderReportApi;
        this.paymentReportApi = paymentReportApi;
        this.diningReportApi = diningReportApi;
    }

    @Override
    public DashboardSummaryApiResponse getDashboardSummary(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(JAKARTA);

        LocalDate fromDate = from == null ? today : from;
        LocalDate toDate = to == null ? today : to;

        if (toDate.isBefore(fromDate)) {
            throw new BadRequestException("'to' date must not be before 'from' date");
        }

        LocalDateTime start = startOfDayInServerZone(fromDate);
        LocalDateTime end = startOfDayInServerZone(toDate.plusDays(1));

        long grossRevenue = paymentReportApi.sumPaidAmount(start, end);
        long paidOrders = paymentReportApi.countPaidOrders(start, end);
        long unpaidOrders = orderReportApi.countUnpaidOrders(start, end);
        long averageOrderValue = paidOrders == 0 ? 0 : grossRevenue / paidOrders;

        Sales sales = new Sales(grossRevenue, paidOrders, unpaidOrders, averageOrderValue);

        Operations operations = new Operations(
            diningReportApi.countOpenDinings(),
            diningReportApi.countOccupiedTables(),
            diningReportApi.countAvailableTables(),
            orderReportApi.countOrdersInProgress()
        );

        List<TopMenuEntry> topMenus = orderReportApi.topMenus(start, end, TOP_MENUS_LIMIT)
            .stream()
            .map(row -> new TopMenuEntry(row.menuId(), row.name(), row.qty(), row.revenue()))
            .toList();

        List<RecentActivityEntry> recentActivity = orderReportApi.recentActivity(RECENT_ACTIVITY_LIMIT)
            .stream()
            .map(row -> new RecentActivityEntry(
                row.orderId(),
                row.orderNumber(),
                row.status(),
                row.paidStatus(),
                row.totalPrice(),
                row.createdAt()
            ))
            .toList();

        return new DashboardSummaryApiResponse(
            new Period(fromDate, toDate),
            sales,
            operations,
            topMenus,
            recentActivity
        );
    }

    private LocalDateTime startOfDayInServerZone(LocalDate date) {
        return date.atStartOfDay()
            .atZone(JAKARTA)
            .withZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime();
    }

}