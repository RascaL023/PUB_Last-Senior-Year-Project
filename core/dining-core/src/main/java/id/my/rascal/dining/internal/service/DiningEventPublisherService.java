package id.my.rascal.dining.internal.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import id.my.rascal.dining.api.event.DiningOrderAddedEvent;
import id.my.rascal.order.api.event.dto.OrderItemSnapshot;

@Service
public class DiningEventPublisherService {

    private final ApplicationEventPublisher eventPublisher;
    private final Logger logger = LoggerFactory.getLogger(DiningEventPublisherService.class);

    public DiningEventPublisherService(
        ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = eventPublisher;
    }

    public void publishOrderAdded(Long diningId, Long orderId, List<OrderItemSnapshot> items) {
        eventPublisher.publishEvent(new DiningOrderAddedEvent(diningId, orderId, List.copyOf(items)));
        logger.info("Published dining event: orderAdded diningId={} orderId={}", diningId, orderId);
    }

}
