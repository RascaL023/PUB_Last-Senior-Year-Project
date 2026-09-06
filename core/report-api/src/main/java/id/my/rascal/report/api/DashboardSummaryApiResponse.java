package id.my.rascal.report.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardSummaryApiResponse(
    Period period,
    Sales sales,
    Operations operations,
    List<TopMenuEntry> topMenus,
    List<RecentActivityEntry> recentActivity
) {

    public record Period(
        LocalDate from,
        LocalDate to
    ) {}

    public record Sales(
        long grossRevenue,
        long paidOrders,
        long unpaidOrders,
        long averageOrderValue
    ) {}

    public record Operations(
        long openDinings,
        long occupiedTables,
        long availableTables,
        long ordersInProgress
    ) {}

    public record TopMenuEntry(
        Long menuId,
        String name,
        long qty,
        long revenue
    ) {}

    public record RecentActivityEntry(
        Long orderId,
        String orderNumber,
        String status,
        String paidStatus,
        long totalPrice,
        LocalDateTime createdAt
    ) {}

}