package id.my.rascal.order.api;

import java.util.Collection;
import java.util.List;

import id.my.rascal.order.api.event.dto.OrderItemSnapshot;

public interface OrderApi {

    OrderApiResponse getOrder(Long id);
    List<OrderApiResponse> getOrders(Collection<Long> ids);
    List<OrderItemSnapshot> getOrderItems(Long orderId);
    OrderApiResponse createOrder(OrderApiCreateRequest request);
    OrderApiResponse confirmOrder(Long id);

}
