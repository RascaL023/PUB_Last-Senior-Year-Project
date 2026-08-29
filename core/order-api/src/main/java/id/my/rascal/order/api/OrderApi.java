package id.my.rascal.order.api;

import java.util.Collection;
import java.util.List;

public interface OrderApi {

    OrderApiResponse getOrder(Long id);
    List<OrderApiResponse> getOrders(Collection<Long> ids);
    void markPaid(Long id);

}
