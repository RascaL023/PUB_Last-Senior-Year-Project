package id.my.rascal.order.internal.adapter;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.order.api.OrderApi;
import id.my.rascal.order.api.OrderApiCreateRequest;
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
    @Transactional(readOnly = true)
    public OrderApiResponse getOrder(Long id) {
        return OrderMapper.toApiResponse(orderQueryService.findActiveOrderById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderApiResponse> getOrders(Collection<Long> ids) {
        return orderQueryService.findActiveOrdersByIds(ids);
    }

    @Override
    @Transactional
    public void markPaid(Long id) {
        orderService.markPaid(id);
    }

    @Override
    @Transactional
    public void markPaid(Collection<Long> ids) {
        orderService.markPaid(ids);
    }

    @Override
    @Transactional
    public OrderApiResponse createOrder(OrderApiCreateRequest request) {
        return OrderMapper.toApiResponse(orderService.createOrder(request));
    }

    @Override
    @Transactional
    public OrderApiResponse confirmOrder(Long id) {
        return OrderMapper.toApiResponse(orderService.confirm(id));
    }

}
