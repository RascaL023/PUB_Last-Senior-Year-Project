package id.my.rascal.dining.api.event;

import java.time.LocalDateTime;
import java.util.List;

import id.my.rascal.order.api.event.dto.OrderItemSnapshot;

public record DiningOrderAddedEvent(
    Long diningId,
    Long orderId,
    String orderNumber,
    Integer totalAmount,
    List<OrderItemSnapshot> items,
    LocalDateTime createdAt
    // TODO(customer-module): sertakan customerId/customerName snapshot setelah
    // customer-api tersedia (kontrak antar-modul, bukan validasi di sini).
) {}
