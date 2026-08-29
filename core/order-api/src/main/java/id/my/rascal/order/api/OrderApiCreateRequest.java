package id.my.rascal.order.api;

import java.util.List;

public record OrderApiCreateRequest(
    OrderTypeApiResponse type,
    Long customerId,
    String customerName,
    String notes,
    List<OrderItemApiRequest> items
) {}
