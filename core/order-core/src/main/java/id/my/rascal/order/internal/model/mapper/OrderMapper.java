package id.my.rascal.order.internal.model.mapper;

import id.my.rascal.order.api.OrderApiResponse;
import id.my.rascal.order.api.OrderTypeApiResponse;
import id.my.rascal.order.internal.entity.Order;
import id.my.rascal.order.internal.entity.OrderItem;
import id.my.rascal.order.internal.entity.OrderItemModifier;
import id.my.rascal.order.internal.model.response.OrderItemModifierResponse;
import id.my.rascal.order.internal.model.response.OrderItemResponse;
import id.my.rascal.order.internal.model.response.OrderResponse;

import java.util.List;

public class OrderMapper {

    private OrderMapper() { }

    public static OrderApiResponse toApiResponse(Order order) {
        OrderTypeApiResponse orderTypeApiResponse =
            OrderTypeApiResponse.valueOf(order.getType().toString());
        return new OrderApiResponse(
            order.getId(),
            orderTypeApiResponse,
            order.getOrderNumber(),
            order.getCustomerId(),
            order.getCustomerName(),
            order.getTotalPrice(),
            order.getCreatedAt()
        );
    }

    public static OrderApiResponse toApiResponse(OrderResponse response) {
        OrderTypeApiResponse orderTypeApiResponse =
            OrderTypeApiResponse.valueOf(response.type().toString());
        return new OrderApiResponse(
            response.id(),
            orderTypeApiResponse,
            response.orderNumber(),
            response.customerId(),
            response.customerName(),
            response.totalPrice(),
            response.createdAt()
        );
    }

    public static OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
            .map(OrderMapper::toItemResponse)
            .toList();

        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getStatus(),
            order.getType(),
            order.getPaidStatus(),
            order.getCustomerId(),
            order.getCustomerName(),
            order.getNotes(),
            order.getTotalPrice(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            items
        );
    }

    public static OrderItemResponse toItemResponse(OrderItem item) {
        List<OrderItemModifierResponse> modifiers = item.getModifiers().stream()
            .map(OrderMapper::toModifierResponse)
            .toList();

        return new OrderItemResponse(
            item.getId(),
            item.getMenuId(),
            item.getItemName(),
            item.getUnitPrice(),
            item.getQuantity(),
            item.getSubtotal(),
            modifiers
        );
    }

    public static OrderItemModifierResponse toModifierResponse(OrderItemModifier modifier) {
        return new OrderItemModifierResponse(
            modifier.getId(),
            modifier.getModifierTypeId(),
            modifier.getModifierOptionId(),
            modifier.getName(),
            modifier.getAdditionalPrice()
        );
    }

}
