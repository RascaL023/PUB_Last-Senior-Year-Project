package id.my.rascal.order.internal.model.request;

import id.my.rascal.order.internal.model.enums.OrderType;

import java.util.List;
import java.util.Optional;

public record OrderPatchRequest(
    Optional<String> customerName,
    Optional<String> notes,
    Optional<OrderType> type,
    Optional<List<OrderItemRequest>> items
) {
    public OrderPatchRequest(
        String customerName, 
        String notes, 
        OrderType type, 
        List<OrderItemRequest> items
    ) {
        this(
            Optional.ofNullable(customerName),
            Optional.ofNullable(notes),
            Optional.ofNullable(type),
            Optional.ofNullable(items)
        );
    }

    public boolean isEmptyPatch() {
        return customerName.isEmpty()
            && notes.isEmpty()
            && items.isEmpty();
    }
}
