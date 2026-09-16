package id.my.rascal.invoice.api.event;

import java.time.LocalDateTime;
import java.util.List;

public record InvoicePaidEvent(
    Long invoiceId,
    String invoiceNumber,
    Long diningId,
    Integer totalAmount,
    Integer paidAmount,
    Integer remainingAmount,
    List<ItemLine> items,
    LocalDateTime paidAt
) {
    public record ItemLine(
        Long menuId,
        String itemName,
        int quantity,
        long amount
    ) {}
}
