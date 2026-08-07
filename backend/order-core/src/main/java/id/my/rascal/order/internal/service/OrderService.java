package id.my.rascal.order.internal.service;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.common.util.StringUtil;
import id.my.rascal.menu.api.MenuDataProvider;
import id.my.rascal.menu.api.MenuSnapshot;
import id.my.rascal.menu.api.ModifierOptionSnapshot;
import id.my.rascal.order.internal.entity.Order;
import id.my.rascal.order.internal.entity.OrderItem;
import id.my.rascal.order.internal.entity.OrderItemModifier;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.enums.PaymentStatus;
import id.my.rascal.order.internal.model.request.OrderItemRequest;
import id.my.rascal.order.internal.model.request.OrderPatchRequest;
import id.my.rascal.order.internal.model.request.OrderPutRequest;
import id.my.rascal.order.internal.model.request.OrderRequest;
import id.my.rascal.order.internal.model.response.OrderItemModifierResponse;
import id.my.rascal.order.internal.model.response.OrderItemResponse;
import id.my.rascal.order.internal.model.response.OrderResponse;
import id.my.rascal.order.internal.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final String ORDER_PREFIX = "ORD-";
    private static final String RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderRepository orderRepository;
    private final MenuDataProvider menuDataProvider;

    public OrderService(OrderRepository orderRepository, MenuDataProvider menuDataProvider) {
        this.orderRepository = orderRepository;
        this.menuDataProvider = menuDataProvider;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        applyCustomer(order, request.customerId(), request.customerName());
        applyNotes(order, request.notes());

        List<OrderItem> items = buildItems(order, request.items());
        order.setOrderItems(items);
        order.setTotalPrice(computeTotalPrice(items));
        order.setCreatedAt(LocalDateTime.now());

        return toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return toResponse(findActiveOrder(id));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> search(
        String keyword,
        OrderStatus status,
        PaymentStatus paymentStatus,
        Pageable pageable
    ) {
        return orderRepository
            .searchActive(StringUtil.normalizeSearch(keyword), status, paymentStatus, pageable)
            .map(this::toResponse);
    }

    @Transactional
    public OrderResponse update(Long id, OrderPutRequest request) {
        Order order = findActiveOrder(id);
        ensureEditable(order);

        applyCustomer(order, request.customerId(), request.customerName());
        applyNotes(order, request.notes());

        replaceItems(order, request.items());

        order.setUpdatedAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus status) {
        if (status == null)
            throw new BadRequestException("Status is required");

        Order order = findActiveOrder(id);
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED)
            throw new BadRequestException("Cannot change status of a " + order.getStatus() + " order");

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse patch(Long id, OrderPatchRequest request) {
        Order order = findActiveOrder(id);

        if (request.status().isPresent()) {
            if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED)
                throw new BadRequestException("Cannot change status of a " + order.getStatus() + " order");

            order.setStatus(request.status().get());
        }

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
            replaceItems(order, request.items().get());
        }

        order.setUpdatedAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public void delete(Long id) {
        Order order = findActiveOrder(id);
        order.setDeletedAt(LocalDateTime.now());
        orderRepository.save(order);
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

    private void replaceItems(Order order, List<OrderItemRequest> itemRequests) {
        List<OrderItem> newItems = buildItems(order, itemRequests);
        order.getOrderItems().clear();
        order.getOrderItems().addAll(newItems);
        order.setTotalPrice(computeTotalPrice(newItems));
    }

    private List<OrderItem> buildItems(Order order, List<OrderItemRequest> itemRequests) {
        List<Long> menuIds = itemRequests.stream()
            .map(OrderItemRequest::menuId)
            .distinct()
            .toList();

        Map<Long, MenuSnapshot> menuMap = menuDataProvider.getMenuSnapshots(menuIds).stream()
            .collect(Collectors.toMap(MenuSnapshot::id, Function.identity()));
        validateSnapshots(menuMap.keySet(), new HashSet<>(menuIds), "Menu");

        List<Long> optionIds = itemRequests.stream()
            .map(OrderItemRequest::modifierOptionIds)
            .filter(java.util.Objects::nonNull)
            .flatMap(Collection::stream)
            .distinct()
            .toList();

        Map<Long, ModifierOptionSnapshot> optionMap = menuDataProvider.getModifierOptionSnapshots(optionIds).stream()
            .collect(Collectors.toMap(ModifierOptionSnapshot::id, Function.identity()));
        validateSnapshots(optionMap.keySet(), new HashSet<>(optionIds), "Modifier option");

        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest itemRequest : itemRequests) {
            MenuSnapshot menu = menuMap.get(itemRequest.menuId());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setMenuId(menu.id());
            item.setItemName(menu.name());
            item.setUnitPrice(menu.basePrice());
            item.setQuantity(itemRequest.quantity());

            int modifierTotal = 0;
            List<OrderItemModifier> modifiers = new ArrayList<>();
            if (itemRequest.modifierOptionIds() != null) {
                for (Long optionId : itemRequest.modifierOptionIds()) {
                    ModifierOptionSnapshot option = optionMap.get(optionId);

                    OrderItemModifier modifier = new OrderItemModifier();
                    modifier.setOrderItem(item);
                    modifier.setModifierTypeId(option.modifierTypeId());
                    modifier.setModifierOptionId(option.id());
                    modifier.setName(option.name());
                    modifier.setAdditionalPrice(option.additionalPrice());

                    modifierTotal += option.additionalPrice();
                    modifiers.add(modifier);
                }
            }

            item.setModifiers(modifiers);
            item.setSubtotal((menu.basePrice() + modifierTotal) * itemRequest.quantity());
            items.add(item);
        }

        return items;
    }

    private void validateSnapshots(Set<Long> foundIds, Set<Long> requestedIds, String label) {
        if (foundIds.size() != requestedIds.size()) {
            Set<Long> missingIds = new HashSet<>(requestedIds);
            missingIds.removeAll(foundIds);
            throw new NotFoundException("Not found " + label + " IDs: " + missingIds);
        }
    }

    private Integer computeTotalPrice(List<OrderItem> items) {
        return items.stream().mapToInt(OrderItem::getSubtotal).sum();
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

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
            .map(this::toItemResponse)
            .toList();

        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getStatus(),
            order.getPaymentStatus(),
            order.getCustomerId(),
            order.getCustomerName(),
            order.getNotes(),
            order.getTotalPrice(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            items
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        List<OrderItemModifierResponse> modifiers = item.getModifiers().stream()
            .map(this::toModifierResponse)
            .toList();

        return new OrderItemResponse(
            item.getId(),
            item.getMenuId(),
            item.getItemName(),
            item.getUnitPrice(),
            item.getQuantity(),
            item.getSubtotal(),
            modifiers
        );
    }

    private OrderItemModifierResponse toModifierResponse(OrderItemModifier modifier) {
        return new OrderItemModifierResponse(
            modifier.getId(),
            modifier.getModifierTypeId(),
            modifier.getModifierOptionId(),
            modifier.getName(),
            modifier.getAdditionalPrice()
        );
    }

}
