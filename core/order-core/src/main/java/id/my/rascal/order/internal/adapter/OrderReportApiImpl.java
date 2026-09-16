package id.my.rascal.order.internal.adapter;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.order.api.OrderReportApi;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.repository.OrderReportRepository;

@Component
public class OrderReportApiImpl implements OrderReportApi {

    private static final List<OrderStatus> IN_PROGRESS = List.of(
        OrderStatus.CREATED,
        OrderStatus.CONFIRMED,
        OrderStatus.PREPARING
    );

    private final OrderReportRepository orderReportRepository;

    public OrderReportApiImpl(OrderReportRepository orderReportRepository) {
        this.orderReportRepository = orderReportRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public long countOrdersInProgress() {
        return orderReportRepository.countByStatusIn(IN_PROGRESS);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentActivityEntry> recentActivity(int limit) {
        return orderReportRepository.findRecentActivity(limit)
            .stream()
            .map(p -> new RecentActivityEntry(
                p.orderId(),
                p.orderNumber(),
                p.status().name(),
                p.totalPrice(),
                p.createdAt()
            ))
            .toList();
    }

}
