package id.my.rascal.order.internal.adapter;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

import id.my.rascal.order.api.OrderApi;
import id.my.rascal.order.api.OrderApiResponse;
import id.my.rascal.order.internal.model.mapper.OrderMapper;
import id.my.rascal.order.internal.service.OrderQueryService;
import id.my.rascal.order.internal.service.OrderService;

@Service
public class OrderApiImpl implements OrderApi {

    private final OrderQueryService orderQueryService;
    private final OrderService orderService;

    public OrderApiImpl(
        OrderQueryService orderQueryService,
        OrderService orderService
    ) {
        this.orderQueryService = orderQueryService;
        this.orderService = orderService;
    }

    @Override
    public OrderApiResponse getOrder(Long id) {
        return OrderMapper.toApiResponse(orderQueryService.findActiveOrderById(id));
    }

    @Override
    public List<OrderApiResponse> getOrders(Collection<Long> ids) {
        return orderQueryService.findActiveOrdersByIds(ids);
    }

    @Override
    public void markPaid(Long id) {
        orderService.markPaid(id);
    }

}
