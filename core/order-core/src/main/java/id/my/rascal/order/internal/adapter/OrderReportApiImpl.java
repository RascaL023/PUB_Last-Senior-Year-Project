package id.my.rascal.order.internal.adapter;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.order.api.OrderReportApi;
import id.my.rascal.order.api.OrderReportApi.RecentActivityEntry;
import id.my.rascal.order.api.OrderReportApi.TopMenuEntry;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.repository.OrderReportRepository;

@Component
public class OrderReportApiImpl implements OrderReportApi {

    private final OrderReportRepository orderReportRepository;

    public OrderReportApiImpl(OrderReportRepository orderReportRepository) {
        this.orderReportRepository = orderReportRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnpaidOrders(LocalDateTime from, LocalDateTime to) {
        return orderReportRepository.countUnpaidNotCancelledInPeriod(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public long countOrdersInProgress() {
        return orderReportRepository.countByStatusIn(
            List.of(OrderStatus.CREATED, OrderStatus.CONFIRMED, OrderStatus.PREPARING)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopMenuEntry> topMenus(LocalDateTime from, LocalDateTime to, int limit) {
        return orderReportRepository.findTopMenus(from, to, limit)
            .stream()
            .map(p -> new TopMenuEntry(
                p.menuId(),
                p.itemName(),
                p.qty(),
                p.revenue()
            ))
            .toList();
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
                p.paidStatus().name(),
                p.totalPrice(),
                p.createdAt()
            ))
            .toList();
    }

}