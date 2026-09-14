package id.my.rascal.order.internal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

import id.my.rascal.order.api.OrderTypeApiResponse;
import id.my.rascal.order.api.event.OrderCancelledEvent;
import id.my.rascal.order.api.event.OrderDeletedEvent;
import id.my.rascal.order.api.event.OrderItemsChangedEvent;
import id.my.rascal.order.api.event.StandaloneOrderCreatedEvent;
import id.my.rascal.order.api.event.dto.OrderItemSnapshot;
import id.my.rascal.order.internal.entity.Order;
import id.my.rascal.order.internal.entity.OrderItem;

@Service
public class OrderEventPublisherService {

    private final ApplicationEventPublisher eventPublisher;
    private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisherService.class);

    public OrderEventPublisherService(
        ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = eventPublisher;
    }

    public void publish(Order order, String eventType) {
        switch (eventType.toLowerCase()) {
            case "create" -> publishCreated(order);
            case "cancel" -> publishCancelled(order);
            case "delete" -> publishDeleted(order);
            default -> logger.error("Unknown eventType: {}", eventType);
        }
        logger.info("Published order event: {}", eventType);
    }


    private void publishCreated(Order order) {
        eventPublisher.publishEvent(new StandaloneOrderCreatedEvent(
            order.getId(),
            order.getOrderNumber(),
            order.getCustomerId(),
            order.getCustomerName(),
            OrderTypeApiResponse.valueOf(order.getType().name()),
            order.getTotalPrice(),
            toSnapshots(order),
            order.getCreatedAt()
        ));
    }

    public void publishItemsChanged(Order order) {
        eventPublisher.publishEvent(new OrderItemsChangedEvent(
            order.getId(),
            order.getOrderNumber(),
            toSnapshots(order),
            order.getUpdatedAt()
        ));
        logger.info("Published order event: itemsChanged orderId={}", order.getId());
    }

    private void publishCancelled(Order order) { 
        eventPublisher.publishEvent(new OrderCancelledEvent(order.getId())); 
    }

    private void publishDeleted(Order order) {
        eventPublisher.publishEvent(new OrderDeletedEvent(order.getId()));
    }

    private List<OrderItemSnapshot> toSnapshots(Order order) {
        return order.getOrderItems().stream()
            .map(OrderEventPublisherService::toSnapshot)
            .toList();
    }

    private static OrderItemSnapshot toSnapshot(OrderItem item) {
        return new OrderItemSnapshot(
            item.getId(),
            item.getMenuId(),
            item.getItemName(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getSubtotal()
        );
    }

}
