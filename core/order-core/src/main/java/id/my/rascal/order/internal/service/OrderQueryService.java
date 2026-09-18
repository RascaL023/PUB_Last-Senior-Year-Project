package id.my.rascal.order.internal.service;

import id.my.rascal.common.exception.ForbiddenException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.common.util.StringUtil;
import id.my.rascal.customer.api.CustomerApi;
import id.my.rascal.customer.api.CustomerApiResponse;
import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.order.api.OrderApiResponse;
import id.my.rascal.order.api.OrderItemDetail;
import id.my.rascal.order.api.event.dto.OrderItemSnapshot;
import id.my.rascal.order.internal.entity.Order;
import id.my.rascal.order.internal.entity.OrderItem;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.mapper.OrderMapper;
import id.my.rascal.order.internal.model.response.GuestOrderTrackingResponse;
import id.my.rascal.order.internal.model.response.OrderResponse;
import id.my.rascal.order.internal.repository.OrderItemRepository;
import id.my.rascal.order.internal.repository.OrderRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerApi customerApi;
    private final InvoiceApi invoiceApi;

    public OrderQueryService(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        CustomerApi customerApi,
        InvoiceApi invoiceApi
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.customerApi = customerApi;
        this.invoiceApi = invoiceApi;
    }

    @Transactional(readOnly = true)
    public OrderResponse findActiveOrderById(Long id) {
        return OrderMapper.toResponse(findActiveOrder(id));
    }

    @Transactional(readOnly = true)
    public List<OrderApiResponse> findActiveOrdersByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty())
            return List.of();

        Map<Long, OrderApiResponse> responseMap = orderRepository.findAllById(ids).stream()
            .filter(o -> o.getDeletedAt() == null)
            .map(OrderMapper::toApiResponse)
            .collect(Collectors.toMap(OrderApiResponse::id, Function.identity()));

        return ids.stream()
            .map(responseMap::get)
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderItemSnapshot> findActiveOrderItems(Long orderId) {
        return findActiveOrder(orderId).getOrderItems().stream()
            .map(item -> new OrderItemSnapshot(
                item.getId(),
                item.getMenuId(),
                item.getItemName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> searchActive(
        String keyword,
        Collection<OrderStatus> statuses,
        Pageable pageable
    ) {
        Collection<OrderStatus> effectiveStatuses = (statuses == null || statuses.isEmpty())
            ? List.of(OrderStatus.values())
            : statuses;

        return orderRepository
            .searchActive(StringUtil.normalizeSearch(keyword), effectiveStatuses, pageable)
            .map(OrderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findMyOrders(Long userAuthId, Pageable pageable) {
        Long customerId = requireCustomerId(userAuthId);
        return orderRepository
            .searchActiveByCustomerId(customerId, pageable)
            .map(OrderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Long requireCustomerId(Long userAuthId) {
        return customerApi.getByUserAuthId(userAuthId)
            .map(CustomerApiResponse::id)
            .orElseThrow(() -> new ForbiddenException("Customer profile not found for this account"));
    }

    @Transactional(readOnly = true)
    public List<Long> findOrderIdsByCustomerId(Long customerId) {
        return orderRepository.findActiveOrderIdsByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<OrderItemDetail>> findItemsByOrderIds(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return Map.of();

        Map<Long, List<OrderItemDetail>> itemsByOrderId = new LinkedHashMap<>();
        for (OrderItem item : orderItemRepository.findActiveByOrderIds(orderIds)) {
            itemsByOrderId
                .computeIfAbsent(item.getOrder().getId(), orderId -> new ArrayList<>())
                .add(OrderMapper.toItemDetail(item));
        }
        return itemsByOrderId;
    }

    @Transactional(readOnly = true)
    public GuestOrderTrackingResponse findActiveByTrackToken(String trackToken) {
        Order order = orderRepository.findActiveByTrackToken(trackToken)
            .orElseThrow(() -> new NotFoundException("Order not found"));

        return new GuestOrderTrackingResponse(
            order.getOrderNumber(),
            order.getType().name(),
            order.getStatus().name(),
            order.getTotalPrice(),
            order.getCreatedAt(),
            invoiceApi.findStatusByOrderId(order.getId()),
            order.getOrderItems().stream()
                .map(OrderMapper::toItemDetail)
                .toList()
        );
    }

    private Order findActiveOrder(Long id) {
        return orderRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));
    }

}
