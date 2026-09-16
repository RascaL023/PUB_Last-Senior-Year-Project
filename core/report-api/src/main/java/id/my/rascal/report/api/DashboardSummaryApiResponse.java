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
        Cash cash,
        Billing billing
    ) {}

    public record Cash(
        long received
    ) {}

    public record Billing(
        long settledInvoices,
        long settledAmount,
        long averageSettledInvoice,
        long outstandingInvoices,
        long outstandingAmount
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
        String billingStatus,
        long orderTotalPrice,
        LocalDateTime createdAt
    ) {}

}
