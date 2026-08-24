package id.my.rascal.order.api;

import java.util.Collection;
import java.util.List;

public interface OrderApi {

    OrderSnapshot getOrder(Long id);
    List<OrderSnapshot> getOrders(Collection<Long> ids);

}
