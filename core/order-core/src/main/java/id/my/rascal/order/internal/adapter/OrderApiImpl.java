package id.my.rascal.order.internal.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.order.api.OrderApi;
import id.my.rascal.order.api.OrderSnapshot;
import id.my.rascal.order.api.OrderTypeSnapshot;
import id.my.rascal.order.internal.entity.Order;
import id.my.rascal.order.internal.repository.OrderRepository;

@Service
public class OrderApiImpl implements OrderApi {

    private final OrderRepository orderRepository;

    public OrderApiImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderSnapshot getOrder(Long id) {
        return toSnapshot(findByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSnapshot> getOrders(Collection<Long> ids) {
        if (ids == null || ids.isEmpty())
            return List.of();

        Map<Long, OrderSnapshot> snapshots = orderRepository.findAllById(ids).stream()
            .filter(o -> o.getDeletedAt() == null)
            .map(this::toSnapshot)
            .collect(Collectors.toMap(OrderSnapshot::id, Function.identity()));

        return ids.stream()
            .map(snapshots::get)
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    @Override
    @Transactional
    public void markPaid(Long id) {
        Order order = findByIdOrThrow(id);
        order.markPaid();
        
        orderRepository.save(order);
    }


    private OrderSnapshot toSnapshot(Order order) {
        OrderTypeSnapshot orderTypeSnapshot = 
            OrderTypeSnapshot.valueOf(order.getType().toString());
        return new OrderSnapshot(
            order.getId(),
            orderTypeSnapshot,
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
