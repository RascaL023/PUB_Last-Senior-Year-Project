package id.my.rascal.order.internal.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import id.my.rascal.order.internal.model.enums.OrderPaidStatus;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.report.OrderRecentActivityProjection;
import id.my.rascal.order.internal.model.report.OrderTopMenuProjection;
import jakarta.persistence.EntityManager;

@Repository
public class OrderReportRepository {

    private final EntityManager entityManager;

    public OrderReportRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countUnpaidNotCancelledInPeriod(LocalDateTime from, LocalDateTime to) {
        Long count = entityManager
            .createQuery("""
                select count(o) from Order o
                where o.deletedAt is null
                  and o.paidStatus = :paidStatus
                  and o.status <> :excludedStatus
                  and o.createdAt >= :from and o.createdAt < :to
                """, Long.class)
            .setParameter("paidStatus", OrderPaidStatus.UNPAID)
            .setParameter("excludedStatus", OrderStatus.CANCELLED)
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult();
        return count == null ? 0L : count;
    }

    public long countByStatusIn(Collection<OrderStatus> statuses) {
        Long count = entityManager
            .createQuery("""
                select count(o) from Order o
                where o.deletedAt is null
                  and o.status in :statuses
                """, Long.class)
            .setParameter("statuses", statuses)
            .getSingleResult();
        return count == null ? 0L : count;
    }

    public List<OrderTopMenuProjection> findTopMenus(LocalDateTime from, LocalDateTime to, int limit) {
        List<Object[]> rows = entityManager
            .createQuery("""
                select oi.menuId, oi.itemName, sum(oi.quantity), sum(oi.subtotal)
                from Order o
                join o.orderItems oi
                where o.deletedAt is null
                  and o.paidStatus = :paidStatus
                  and o.status <> :excludedStatus
                  and o.createdAt >= :from and o.createdAt < :to
                group by oi.menuId, oi.itemName
                order by sum(oi.subtotal) desc
                """, Object[].class)
            .setParameter("paidStatus", OrderPaidStatus.PAID)
            .setParameter("excludedStatus", OrderStatus.CANCELLED)
            .setParameter("from", from)
            .setParameter("to", to)
            .setMaxResults(limit)
            .getResultList();

        return rows.stream()
            .map(row -> new OrderTopMenuProjection(
                (Long) row[0],
                (String) row[1],
                (Long) row[2],
                (Long) row[3]
            ))
            .toList();
    }

    public List<OrderRecentActivityProjection> findRecentActivity(int limit) {
        List<Object[]> rows = entityManager
            .createQuery("""
                select o.id, o.orderNumber, o.status, o.paidStatus, o.totalPrice, o.createdAt
                from Order o
                where o.deletedAt is null
                order by o.createdAt desc
                """, Object[].class)
            .setMaxResults(limit)
            .getResultList();

        return rows.stream()
            .map(row -> new OrderRecentActivityProjection(
                (Long) row[0],
                (String) row[1],
                (OrderStatus) row[2],
                (OrderPaidStatus) row[3],
                (Integer) row[4],
                (LocalDateTime) row[5]
            ))
            .toList();
    }

    public List<OrderItemLineRow> findItemLinesByOrderIds(Collection<Long> orderIds) {
        List<Object[]> rows = entityManager
            .createQuery("""
                select oi.order.id, oi.menuId, oi.itemName, oi.quantity, oi.subtotal
                from OrderItem oi
                where oi.order.id in :orderIds
                  and oi.order.deletedAt is null
                """, Object[].class)
            .setParameter("orderIds", orderIds)
            .getResultList();

        return rows.stream()
            .map(row -> new OrderItemLineRow(
                (Long) row[0],
                (Long) row[1],
                (String) row[2],
                (Integer) row[3],
                (Integer) row[4]
            ))
            .toList();
    }

    public record OrderItemLineRow(
        Long orderId,
        Long menuId,
        String itemName,
        Integer quantity,
        Integer subtotal
    ) {}

}
