package id.my.rascal.order.api;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderReportApi {

    long countOrdersInProgress();
    List<RecentActivityEntry> recentActivity(int limit);

    record RecentActivityEntry(
        Long orderId,
        String orderNumber,
        String status,
        int orderTotalPrice,
        LocalDateTime createdAt
    ) {}

}
