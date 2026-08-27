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
import id.my.rascal.order.api.OrderApiCreateRequest;
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
    private final id.my.rascal.order.internal.service.OrderService orderService;

    public OrderApiImpl(
        OrderRepository orderRepository,
        OrderStatusFlowPolicy orderStatusFlowPolicy,
        id.my.rascal.order.internal.service.OrderService orderService
    ) {
        this.orderRepository = orderRepository;
        this.orderStatusFlowPolicy = orderStatusFlowPolicy;
        this.orderService = orderService;
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

    @Override
    @Transactional
    public OrderApiResponse createDineInOrder(Long diningId, OrderApiCreateRequest request) {
        var response = orderService.createDineInOrder(diningId, request);
        return toApiResponse(response);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderApiResponse> getOrdersByDiningId(Long diningId) {
        return orderRepository.findActiveByDiningId(diningId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderApiResponse> getOrdersByDiningIds(java.util.Collection<Long> diningIds) {
        if (diningIds == null || diningIds.isEmpty())
            return List.of();

        return orderRepository.findActiveByDiningIds(diningIds).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public void markDiningOrdersPaid(Long diningId) {
        List<Order> orders = orderRepository.findActiveByDiningId(diningId);
        for (Order order : orders) {
            if (order.getPaidStatus() != id.my.rascal.order.internal.model.enums.OrderPaidStatus.PAID) {
                order.markPaid();
                order.setUpdatedAt(LocalDateTime.now());
            }
        }
        orderRepository.saveAll(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<Long, Integer> getDiningTotals(Collection<Long> diningIds) {
        if (diningIds == null || diningIds.isEmpty())
            return java.util.Map.of();

        List<Order> orders = orderRepository.findActiveByDiningIds(diningIds);
        return orders.stream()
            .filter(o -> o.getDiningId() != null)
            .filter(o -> o.getDeletedAt() == null)
            .filter(o -> o.getStatus() != id.my.rascal.order.internal.model.enums.OrderStatus.CANCELLED)
            .collect(java.util.stream.Collectors.groupingBy(
                Order::getDiningId,
                java.util.stream.Collectors.summingInt(Order::getTotalPrice)
            ));
    }


    private OrderApiResponse toResponse(Order order) {
        OrderTypeApiResponse orderTypeApiResponse = 
            OrderTypeApiResponse.valueOf(order.getType().toString());
        return new OrderApiResponse(
            order.getId(),
            orderTypeApiResponse,
            order.getStatus().name(),
            order.getPaidStatus().name(),
            order.getOrderNumber(),
            order.getCustomerId(),
            order.getCustomerName(),
            order.getDiningId(),
            order.getTotalPrice(),
            order.getCreatedAt()
        );
    }

    private OrderApiResponse toApiResponse(id.my.rascal.order.internal.model.response.OrderResponse response) {
        OrderTypeApiResponse orderTypeApiResponse = 
            OrderTypeApiResponse.valueOf(response.type().name());
        return new OrderApiResponse(
            response.id(),
            orderTypeApiResponse,
            response.status().name(),
            response.paidStatus().name(),
            response.orderNumber(),
            response.customerId(),
            response.customerName(),
            response.diningId(),
            response.totalPrice(),
            response.createdAt()
        );
    }

    private Order findByIdOrThrow(Long id) {
        return orderRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));
    }

}
