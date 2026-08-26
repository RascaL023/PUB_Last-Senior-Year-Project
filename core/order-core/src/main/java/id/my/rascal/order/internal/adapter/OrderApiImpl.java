package id.my.rascal.order.internal.adapter;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.order.api.OrderApi;
import id.my.rascal.order.api.OrderApiResponse;
import id.my.rascal.order.api.OrderTypeApiResponse;
import id.my.rascal.order.internal.entity.Order;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.enums.OrderType;
import id.my.rascal.order.internal.repository.OrderRepository;
import id.my.rascal.order.internal.service.OrderStatusFlowPolicy;

@Service
public class OrderApiImpl implements OrderApi {

    private final OrderRepository orderRepository;
    private final OrderStatusFlowPolicy orderStatusFlowPolicy;

    public OrderApiImpl(
        OrderRepository orderRepository,
        OrderStatusFlowPolicy orderStatusFlowPolicy
    ) {
        this.orderRepository = orderRepository;
        this.orderStatusFlowPolicy = orderStatusFlowPolicy;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderApiResponse getOrder(Long id) {
        return toResponse(findByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderApiResponse> getOrders(Collection<Long> ids) {
        if (ids == null || ids.isEmpty())
            return List.of();

        Map<Long, OrderApiResponse> responseMap = orderRepository.findAllById(ids).stream()
            .filter(o -> o.getDeletedAt() == null)
            .map(this::toResponse)
            .collect(Collectors.toMap(OrderApiResponse::id, Function.identity()));

        return ids.stream()
            .map(responseMap::get)
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    @Override
    @Transactional
    public void markPaid(Long id) {
        Order order = findByIdOrThrow(id);
        order.markPaid();

        if (order.getType().equals(OrderType.TAKEAWAY)) {
            orderStatusFlowPolicy.validateTransition(order, OrderStatus.CONFIRMED);
            order.markConfirmed();
        }

        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }


    private OrderApiResponse toResponse(Order order) {
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

    private Order findByIdOrThrow(Long id) {
        return orderRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));
    }

}
