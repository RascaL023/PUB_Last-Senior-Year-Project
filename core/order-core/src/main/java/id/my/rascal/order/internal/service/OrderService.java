package id.my.rascal.order.internal.service;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.ConflictException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.common.util.StringUtil;
import id.my.rascal.order.internal.entity.Order;
import id.my.rascal.order.internal.entity.OrderItem;
import id.my.rascal.order.internal.model.enums.OrderPaidStatus;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.enums.OrderType;
import id.my.rascal.order.internal.model.mapper.OrderMapper;
import id.my.rascal.order.internal.model.request.OrderPatchRequest;
import id.my.rascal.order.internal.model.request.OrderPutRequest;
import id.my.rascal.order.api.OrderApiCreateRequest;
import id.my.rascal.order.internal.model.request.OrderItemModifierRequest;
import id.my.rascal.order.internal.model.request.OrderItemRequest;
import id.my.rascal.order.internal.model.request.OrderRequest;
import id.my.rascal.order.internal.model.response.OrderResponse;
import id.my.rascal.order.internal.repository.OrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Random;

@Service
public class OrderService {

    private static final String ORDER_PREFIX = "ORD-";
    private static final String RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyyyy");

    private final OrderRepository orderRepository;
    private final OrderItemService orderItemService;
    private final OrderStatusFlowPolicy orderStatusFlowPolicy;

    public OrderService(
        OrderRepository orderRepository,
        OrderItemService orderItemService,
        OrderStatusFlowPolicy orderStatusFlowPolicy
    ) {
        this.orderRepository = orderRepository;
        this.orderItemService = orderItemService;
        this.orderStatusFlowPolicy = orderStatusFlowPolicy;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        applyCustomer(order, request.customerId(), request.customerName());
        applyNotes(order, request.notes());

        List<OrderItem> items = orderItemService.buildItems(order, request.items());
        order.setOrderItems(items);
        order.setTotalPrice(orderItemService.computeTotalPrice(items));
        order.setCreatedAt(LocalDateTime.now());
        order.setType(request.type());
        order.markUnpaid();
        order.markCreated();

        return OrderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse update(Long id, OrderPutRequest request) {
        Order order = findActiveOrder(id);
        ensureEditable(order);

        applyCustomer(order, request.customerId(), request.customerName());
        applyNotes(order, request.notes());

        orderItemService.replaceItems(order, request.items());

        order.setType(request.type());
        order.setUpdatedAt(LocalDateTime.now());
        return OrderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse patch(Long id, OrderPatchRequest request) {
        Order order = findActiveOrder(id);

        if (request.customerName().isPresent()) {
            ensureEditable(order);
            applyCustomer(order, order.getCustomerId(), request.customerName().get());
        }

        if (request.notes().isPresent()) {
            ensureEditable(order);
            applyNotes(order, request.notes().get());
        }

        if (request.items().isPresent()) {
            ensureEditable(order);
            orderItemService.replaceItems(order, request.items().get());
        }

        if (request.type().isPresent()) {
            ensureEditable(order);
            order.setType(request.type().get());
        }

        order.setUpdatedAt(LocalDateTime.now());
        return OrderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public void delete(Long id) {
        Order order = findActiveOrder(id);
        // TODO: restore stock if exists

        order.setDeletedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Transactional
    public OrderResponse createOrder(OrderApiCreateRequest request) {
        if (request.type() == null) throw new BadRequestException("Order type cannot be null");
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        applyCustomer(order, request.customerId(), request.customerName());
        applyNotes(order, request.notes());

        List<OrderItemRequest> itemRequests = request.items() == null ? List.of()
            : request.items().stream()
                .map(apiReq -> new OrderItemRequest(
                    null,
                    apiReq.menuId(),
                    apiReq.quantity(),
                    apiReq.modifiers() == null ? List.of()
                        : apiReq.modifiers().stream()
                            .map(m -> new OrderItemModifierRequest(null, m.modifierOptionId()))
                            .toList()
                ))
                .toList();

        List<OrderItem> items = orderItemService.buildItems(order, itemRequests);
        order.setOrderItems(items);
        order.setTotalPrice(orderItemService.computeTotalPrice(items));
        order.setCreatedAt(LocalDateTime.now());
        order.setType(OrderType.valueOf(request.type().name()));
        order.markUnpaid();
        order.markCreated();

        return OrderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse confirm(Long id) {
        Order order = findActiveOrder(id);
        orderStatusFlowPolicy.validateTransition(order, OrderStatus.CONFIRMED);

        order.markConfirmed();
        order.setUpdatedAt(LocalDateTime.now());
        return OrderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public void markPaid(Long id) {
        Order order = findActiveOrder(id);
        if (order.getPaidStatus().equals(OrderPaidStatus.PAID))
            throw new ConflictException("Order already paid");
        order.markPaid();

        if (order.getType().equals(OrderType.TAKEAWAY)) {
            orderStatusFlowPolicy.validateTransition(order, OrderStatus.CONFIRMED);
            order.markConfirmed();
        }

        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Transactional
    public void markPaid(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return;
        orderRepository.markPaidByIds(orderIds, OrderPaidStatus.PAID, LocalDateTime.now());
    }

    @Transactional
    public OrderResponse preparing(Long id) {
        OrderStatus nextStatus = OrderStatus.PREPARING;
        Order order = findActiveOrder(id);
        orderStatusFlowPolicy.validateTransition(order, nextStatus);

        order.markPreparing();
        order.setUpdatedAt(LocalDateTime.now());
        return OrderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse ready(Long id) {
        OrderStatus nextStatus = OrderStatus.READY;
        Order order = findActiveOrder(id);
        orderStatusFlowPolicy.validateTransition(order, nextStatus);

        order.markReady();
        order.setUpdatedAt(LocalDateTime.now());
        return OrderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse complete(Long id) {
        OrderStatus nextStatus = OrderStatus.COMPLETED;
        Order order = findActiveOrder(id);
        orderStatusFlowPolicy.validateTransition(order, nextStatus);

        order.markCompleted();
        order.setUpdatedAt(LocalDateTime.now());
        return OrderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        OrderStatus nextStatus = OrderStatus.CANCELLED;
        Order order = findActiveOrder(id);
        orderStatusFlowPolicy.validateTransition(order, nextStatus);
        // TODO: restore stock if exists

        order.markCancelled();
        order.setUpdatedAt(LocalDateTime.now());
        return OrderMapper.toResponse(orderRepository.save(order));
    }


    private Order findActiveOrder(Long id) {
        return orderRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));
    }

    private void ensureEditable(Order order) {
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED)
            throw new BadRequestException("Cannot modify a " + order.getStatus() + " order");
    }

    private void applyCustomer(Order order, Long customerId, String customerName) {
        order.setCustomerId(customerId);
        order.setCustomerName(StringUtil.safeIsBlank(customerName) ? null : StringUtil.normalizeSpaces(customerName));
    }

    private void applyNotes(Order order, String notes) {
        order.setNotes(StringUtil.safeIsBlank(notes) ? null : StringUtil.normalizeSpaces(notes));
    }

    private String generateOrderNumber() {
        for (int i = 0; i < 10; i++) {
            String candidate = ORDER_PREFIX + LocalDateTime.now().format(DATE_FORMAT) + "-" + randomSuffix(6);
            if (!orderRepository.existsByOrderNumber(candidate))
                return candidate;
        }
        throw new IllegalStateException("Failed to generate unique order number");
    }

    private static String randomSuffix(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(RANDOM_CHARS.charAt(RANDOM.nextInt(RANDOM_CHARS.length())));
        return sb.toString();
    }

}
