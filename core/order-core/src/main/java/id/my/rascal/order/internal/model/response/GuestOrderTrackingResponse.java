package id.my.rascal.order.internal.model.response;

import java.time.LocalDateTime;
import java.util.List;

import id.my.rascal.order.api.OrderItemDetail;

public record GuestOrderTrackingResponse(
    String orderNumber,
    String type,
    String status,
    Integer totalPrice,
    LocalDateTime createdAt,
    String invoiceStatus,
    List<OrderItemDetail> items
) {}
