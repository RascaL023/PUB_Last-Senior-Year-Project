package id.my.rascal.order.api;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only contract order untuk report/dashboard.
 *
 * Order tidak lagi punya status pembayaran — settlement dimiliki invoice
 * ({@code InvoiceReportApi}), jadi contract ini sengaja tidak mengekspos field finansial.
 */
public interface OrderReportApi {

    /** Order aktif yang masih diproses dapur/meja. */
    long countOrdersInProgress();

    /** Order terbaru (tanpa filter periode) untuk kartu "aktivitas terbaru". */
    List<RecentActivityEntry> recentActivity(int limit);

    record RecentActivityEntry(
        Long orderId,
        String orderNumber,
        String status,
        int totalPrice,
        LocalDateTime createdAt
    ) {}

}
