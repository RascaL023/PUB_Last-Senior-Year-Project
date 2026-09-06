package id.my.rascal.order.api;

import java.time.LocalDateTime;
import java.util.List;

public record OrderPaidEvent(
    Long orderId,
    LocalDateTime paidAt,
    List<ItemLine> items
) {

    public record ItemLine(
        Long menuId,
        String itemName,
        int quantity,
        long subtotal
    ) {}

}
