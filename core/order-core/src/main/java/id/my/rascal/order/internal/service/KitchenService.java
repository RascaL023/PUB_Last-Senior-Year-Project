package id.my.rascal.order.internal.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.dining.api.DiningReportApi;
import id.my.rascal.order.internal.entity.Order;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.mapper.OrderMapper;
import id.my.rascal.order.internal.model.response.KitchenTicketResponse;
import id.my.rascal.order.internal.model.response.OrderItemResponse;
import id.my.rascal.order.internal.repository.OrderItemRepository;
import id.my.rascal.order.internal.repository.OrderRepository;

@Service
public class KitchenService {

    private static final List<OrderStatus> DEFAULT_QUEUE = List.of(
        OrderStatus.CONFIRMED,
        OrderStatus.PREPARING
    );

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DiningReportApi diningReportApi;

    public KitchenService(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        DiningReportApi diningReportApi
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.diningReportApi = diningReportApi;
    }

    @Transactional(readOnly = true)
    public List<KitchenTicketResponse> getQueue(Collection<OrderStatus> statuses, int size) {
        List<OrderStatus> queueStatuses = (statuses == null || statuses.isEmpty())
            ? DEFAULT_QUEUE
            : List.copyOf(statuses);

        Page<Order> page = orderRepository.searchActive(
            null,
            queueStatuses,
            PageRequest.of(0, effectiveSize(size), Sort.by(Sort.Direction.ASC, "createdAt"))
        );

        List<Order> orders = page.getContent();
        if (orders.isEmpty()) return List.of();

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, String> tableNumbers = diningReportApi.tableNumbersByOrderIds(orderIds);

        Map<Long, List<OrderItemResponse>> itemsByOrderId = orderItemRepository
            .findActiveByOrderIds(orderIds)
            .stream()
            .collect(Collectors.groupingBy(
                item -> item.getOrder().getId(),
                Collectors.mapping(OrderMapper::toItemResponse, Collectors.toList())
            ));

        return orders.stream()
            .map(order -> new KitchenTicketResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getType(),
                order.getStatus(),
                tableNumbers.get(order.getId()),
                order.getNotes(),
                order.getCreatedAt(),
                itemsByOrderId.getOrDefault(order.getId(), List.of())
            ))
            .toList();
    }

    private int effectiveSize(int size) {
        if (size <= 0) return DEFAULT_SIZE;
        return Math.min(size, MAX_SIZE);
    }

}
