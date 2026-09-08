package id.my.rascal.order.internal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import id.my.rascal.order.api.OrderTypeApiResponse;
import id.my.rascal.order.api.event.OrderCancelledEvent;
import id.my.rascal.order.api.event.StandaloneOrderCreatedEvent;
import id.my.rascal.order.api.event.dto.OrderItemSnapshot;
import id.my.rascal.order.internal.entity.Order;

@Service
public class OrderEventPublisherService {

    private final ApplicationEventPublisher eventPublisher;
    private final Logger logger = LoggerFactory.getLogger(OrderEventPublisherService.class);

    public OrderEventPublisherService(
        ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = eventPublisher;
    }

    public void publish(Order order, String eventType) {
        switch (eventType.toLowerCase()) {
            case "create" -> publishCreated(order);
            case "cancel" -> publishCancelled(order);
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
            order.getOrderItems().stream()
                .map(item -> new OrderItemSnapshot(
                    item.getId(),
                    item.getMenuId(),
                    item.getItemName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal()
                ))
                .toList(),
            order.getCreatedAt()
        ));
    }

    private void publishCancelled(Order order) { 
        eventPublisher.publishEvent(new OrderCancelledEvent(order.getId())); 
    }

}
