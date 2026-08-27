package id.my.rascal.order.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface OrderApi {

    OrderApiResponse getOrder(Long id);
    List<OrderApiResponse> getOrders(Collection<Long> ids);
    void markPaid(Long id);
    OrderApiResponse createDineInOrder(Long diningId, OrderApiCreateRequest request);
    List<OrderApiResponse> getOrdersByDiningId(Long diningId);
    List<OrderApiResponse> getOrdersByDiningIds(Collection<Long> diningIds);
    void markDiningOrdersPaid(Long diningId);
    Map<Long, Integer> getDiningTotals(Collection<Long> diningIds);
}
