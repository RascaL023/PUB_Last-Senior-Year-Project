package id.my.rascal.order.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import id.my.rascal.order.api.event.dto.OrderItemSnapshot;

public interface OrderApi {

    OrderApiResponse getOrder(Long id);
    List<OrderApiResponse> getOrders(Collection<Long> ids);
    List<OrderItemSnapshot> getOrderItems(Long orderId);
    List<Long> findOrderIdsByCustomerId(Long customerId);
    Map<Long, List<OrderItemDetail>> getItemsByOrderIds(Collection<Long> orderIds);
    OrderApiResponse createOrder(OrderApiCreateRequest request);
    OrderApiResponse confirmOrder(Long id);

}
