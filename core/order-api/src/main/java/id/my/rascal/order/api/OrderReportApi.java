package id.my.rascal.order.api;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderReportApi {

    long countUnpaidOrders(LocalDateTime from, LocalDateTime to);
    long countOrdersInProgress();
    List<TopMenuEntry> topMenus(LocalDateTime from, LocalDateTime to, int limit);
    List<RecentActivityEntry> recentActivity(int limit);

    record TopMenuEntry(
        Long menuId,
        String name,
        long qty,
        long revenue
    ) {}

    record RecentActivityEntry(
        Long orderId,
        String orderNumber,
        String status,
        String paidStatus,
        long totalPrice,
        LocalDateTime createdAt
    ) {}

}
