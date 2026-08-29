package id.my.rascal.order.internal.service;

import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.common.util.StringUtil;
import id.my.rascal.order.api.OrderApiResponse;
import id.my.rascal.order.internal.entity.Order;
import id.my.rascal.order.internal.model.enums.OrderPaidStatus;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.mapper.OrderMapper;
import id.my.rascal.order.internal.model.response.OrderResponse;
import id.my.rascal.order.internal.repository.OrderRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public OrderQueryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
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
    public Page<OrderResponse> searchActive(
        String keyword,
        OrderStatus status,
        OrderPaidStatus paidStatus,
        Pageable pageable
    ) {
        return orderRepository
            .searchActive(StringUtil.normalizeSearch(keyword), status, paidStatus, pageable)
            .map(OrderMapper::toResponse);
    }

    private Order findActiveOrder(Long id) {
        return orderRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));
    }

}
