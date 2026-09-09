package id.my.rascal.dining.internal.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import id.my.rascal.dining.api.event.DiningClosedEvent;
import id.my.rascal.dining.api.event.DiningOpenedEvent;
import id.my.rascal.dining.api.event.DiningOrderAddedEvent;
import id.my.rascal.dining.internal.entity.Dining;
import id.my.rascal.order.api.event.dto.OrderItemSnapshot;

@Service
public class DiningEventPublisherService {

    private final ApplicationEventPublisher eventPublisher;
    private static final Logger logger = LoggerFactory.getLogger(DiningEventPublisherService.class);

    public DiningEventPublisherService(
        ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = eventPublisher;
    }

    public void publishOrderAdded(
        Long diningId,
        Long orderId,
        String orderNumber,
        Integer totalAmount,
        List<OrderItemSnapshot> items,
        LocalDateTime createdAt
    ) {
        eventPublisher.publishEvent(new DiningOrderAddedEvent(
            diningId, orderId, orderNumber, totalAmount, List.copyOf(items), createdAt
        ));
        logger.info("Published dining event: orderAdded diningId={} orderId={}", diningId, orderId);
    }

    public void publishOpened(Dining dining, String tableNumber) {
        eventPublisher.publishEvent(new DiningOpenedEvent(
            dining.getId(),
            dining.getTableId(),
            tableNumber,
            dining.getCreatedAt()
        ));
        logger.info("Published dining event: opened diningId={}", dining.getId());
    }

    public void publishClosed(Dining dining) {
        eventPublisher.publishEvent(new DiningClosedEvent(
            dining.getId(),
            dining.getClosedAt()
        ));
        logger.info("Published dining event: closed diningId={}", dining.getId());
    }

}
