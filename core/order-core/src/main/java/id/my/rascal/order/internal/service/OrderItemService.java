package id.my.rascal.order.internal.service;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.menu.api.MenuApi;
import id.my.rascal.menu.api.MenuApiResponse;
import id.my.rascal.menu.api.ModifierOptionApiResponse;
import id.my.rascal.menu.api.ModifierTypeApiResponse;
import id.my.rascal.order.internal.entity.Order;
import id.my.rascal.order.internal.entity.OrderItem;
import id.my.rascal.order.internal.entity.OrderItemModifier;
import id.my.rascal.order.internal.model.request.OrderItemModifierRequest;
import id.my.rascal.order.internal.model.request.OrderItemRequest;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderItemService {

    private final MenuApi menuApi;

    public OrderItemService(MenuApi menuApi) {
        this.menuApi = menuApi;
    }

    public List<OrderItem> buildItems(Order order, List<OrderItemRequest> itemRequests) {
        Snapshots snapshots = fetchSnapshots(itemRequests);
        Map<Long, Long> optionToTypeId = snapshots.optionToTypeId();

        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest itemRequest : itemRequests) {
            MenuApiResponse menu = snapshots.menuMap().get(itemRequest.menuId());
            if (!menu.isAvailable()) throw new BadRequestException("Menu " + menu.name() + " is not available");

            validateItemModifiers(menu, optionToTypeId, itemRequest.modifiers());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setMenuId(menu.id());
            item.setItemName(menu.name());
            item.setUnitPrice(menu.basePrice());
            item.setQuantity(itemRequest.quantity());

            int modifierTotal = 0;
            List<OrderItemModifier> modifiers = new ArrayList<>();
            if (itemRequest.modifiers() != null) {
                for (OrderItemModifierRequest mReq : itemRequest.modifiers()) {
                    ModifierOptionApiResponse option = snapshots.optionMap().get(mReq.modifierOptionId());

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

    public void replaceItems(Order order, List<OrderItemRequest> itemRequests) {
        Snapshots snapshots = fetchSnapshots(itemRequests);
        reconcileItems(order, itemRequests, snapshots);
        order.setTotalPrice(computeTotalPrice(order.getOrderItems()));
    }

    public Integer computeTotalPrice(List<OrderItem> items) {
        return items.stream().mapToInt(OrderItem::getSubtotal).sum();
    }

    private void reconcileItems(Order order, List<OrderItemRequest> itemRequests, Snapshots snapshots) {
        Map<Long, Long> optionToTypeId = snapshots.optionToTypeId();

        Set<Long> incomingItemIds = itemRequests.stream()
            .map(OrderItemRequest::id)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
        order.getOrderItems().removeIf(item -> item.getId() != null && !incomingItemIds.contains(item.getId()));

        for (OrderItemRequest itemRequest : itemRequests) {
            MenuApiResponse menu = snapshots.menuMap().get(itemRequest.menuId());
            validateItemModifiers(menu, optionToTypeId, itemRequest.modifiers());

            OrderItem item;
            if (itemRequest.id() != null) {
                item = order.getOrderItems().stream()
                    .filter(i -> itemRequest.id().equals(i.getId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException(
                        "Order item " + itemRequest.id() + " not found in order " + order.getId()));
                item.setMenuId(menu.id());
                item.setItemName(menu.name());
                item.setUnitPrice(menu.basePrice());
                item.setQuantity(itemRequest.quantity());
            } else {
                item = new OrderItem();
                item.setOrder(order);
                item.setMenuId(menu.id());
                item.setItemName(menu.name());
                item.setUnitPrice(menu.basePrice());
                item.setQuantity(itemRequest.quantity());
                order.getOrderItems().add(item);
            }

            Set<Long> incomingModIds = itemRequest.modifiers() == null ? Set.of()
                : itemRequest.modifiers().stream()
                    .map(OrderItemModifierRequest::id)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            item.getModifiers().removeIf(m -> m.getId() != null && !incomingModIds.contains(m.getId()));

            int modifierTotal = 0;
            if (itemRequest.modifiers() != null) {
                for (OrderItemModifierRequest mReq : itemRequest.modifiers()) {
                    ModifierOptionApiResponse option = snapshots.optionMap().get(mReq.modifierOptionId());
                    OrderItemModifier mod;
                    if (mReq.id() != null) {
                        mod = item.getModifiers().stream()
                            .filter(m -> mReq.id().equals(m.getId()))
                            .findFirst()
                            .orElseThrow(() -> new BadRequestException(
                                "Order item modifier " + mReq.id() + " not found in order " + order.getId()));
                    } else {
                        mod = new OrderItemModifier();
                        mod.setOrderItem(item);
                    }
                    mod.setModifierTypeId(option.modifierTypeId());
                    mod.setModifierOptionId(option.id());
                    mod.setName(option.name());
                    mod.setAdditionalPrice(option.additionalPrice());
                    if (mReq.id() == null) {
                        item.getModifiers().add(mod);
                    }
                    modifierTotal += option.additionalPrice();
                }
            }

            item.setSubtotal((menu.basePrice() + modifierTotal) * itemRequest.quantity());
        }
    }

    private void validateItemModifiers(
        MenuApiResponse menu,
        Map<Long, Long> optionToTypeId,
        List<OrderItemModifierRequest> modifiers
    ) {
        Map<Long, ModifierTypeApiResponse> allowedTypes = Optional.ofNullable(menu.modifierTypes())
            .orElse(List.of()).stream()
            .collect(Collectors.toMap(ModifierTypeApiResponse::id, Function.identity()));

        Map<Long, Long> typeCounts = modifiers == null
            ? Map.of()
            : modifiers.stream()
                .map(m -> optionToTypeId.get(m.modifierOptionId()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        for (Long typeId : typeCounts.keySet()) {
            if (!allowedTypes.containsKey(typeId)) {
                throw new BadRequestException(
                    "Modifier option of type " + typeId + " is not available for menu " + menu.id());
            }
        }

        for (ModifierTypeApiResponse type : allowedTypes.values()) {
            long count = typeCounts.getOrDefault(type.id(), 0L);
            if (count > type.maxSelection()) {
                throw new BadRequestException(
                    "Modifier type " + type.id() + " allows at most " + type.maxSelection() + " selection(s)");
            }
            if (count < type.minSelection()) {
                throw new BadRequestException(
                    "Modifier type " + type.id() + " requires at least " + type.minSelection() + " selection(s)");
            }
        }
    }

    private Snapshots fetchSnapshots(List<OrderItemRequest> itemRequests) {
        List<Long> menuIds = itemRequests.stream()
            .map(OrderItemRequest::menuId)
            .distinct()
            .toList();

        Map<Long, MenuApiResponse> menuMap = menuApi.getMenuSnapshots(menuIds).stream()
            .collect(Collectors.toMap(MenuApiResponse::id, Function.identity()));
        validateSnapshots(menuMap.keySet(), new HashSet<>(menuIds), "Menu");

        List<Long> optionIds = itemRequests.stream()
            .map(OrderItemRequest::modifiers)
            .filter(java.util.Objects::nonNull)
            .flatMap(Collection::stream)
            .map(OrderItemModifierRequest::modifierOptionId)
            .distinct()
            .toList();

        Map<Long, ModifierOptionApiResponse> optionMap = menuApi.getModifierOptionSnapshots(optionIds).stream()
            .collect(Collectors.toMap(ModifierOptionApiResponse::id, Function.identity()));
        validateSnapshots(optionMap.keySet(), new HashSet<>(optionIds), "Modifier option");

        Map<Long, Long> optionToTypeId = optionMap.values().stream()
            .collect(Collectors.toMap(ModifierOptionApiResponse::id, ModifierOptionApiResponse::modifierTypeId));

        return new Snapshots(menuMap, optionMap, optionToTypeId);
    }

    private record Snapshots(
        Map<Long, MenuApiResponse> menuMap,
        Map<Long, ModifierOptionApiResponse> optionMap,
        Map<Long, Long> optionToTypeId
    ) {}

    private void validateSnapshots(Set<Long> foundIds, Set<Long> requestedIds, String label) {
        if (foundIds.size() != requestedIds.size()) {
            Set<Long> missingIds = new HashSet<>(requestedIds);
            missingIds.removeAll(foundIds);
            throw new NotFoundException("Not found " + label + " IDs: " + missingIds);
        }
    }

}
