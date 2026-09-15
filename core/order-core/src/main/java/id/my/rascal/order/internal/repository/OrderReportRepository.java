package id.my.rascal.order.internal.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.report.OrderRecentActivityProjection;
import jakarta.persistence.EntityManager;

// Read only — agregasi untuk report-core lewat OrderReportApi.
@Repository
public class OrderReportRepository {

    private final EntityManager entityManager;

    public OrderReportRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
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

    public List<OrderRecentActivityProjection> findRecentActivity(int limit) {
        List<Object[]> rows = entityManager
            .createQuery("""
                select o.id, o.orderNumber, o.status, o.totalPrice, o.createdAt
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
                (Integer) row[3],
                (LocalDateTime) row[4]
            ))
            .toList();
    }

}
