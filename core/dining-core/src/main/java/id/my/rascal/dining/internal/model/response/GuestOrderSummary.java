package id.my.rascal.dining.internal.model.response;

import java.time.LocalDateTime;
import java.util.List;

import id.my.rascal.order.api.OrderItemDetail;

public record GuestOrderSummary(
    String orderNumber,
    String status,
    Integer totalPrice,
    LocalDateTime createdAt,
    List<OrderItemDetail> items
) {}
