package id.my.rascal.dining.internal.repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import id.my.rascal.dining.internal.entity.DiningStatus;
import id.my.rascal.dining.internal.entity.TableStatus;
import jakarta.persistence.EntityManager;

@Repository
public class DiningReportRepository {

    private final EntityManager entityManager;

    public DiningReportRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countByStatus(DiningStatus status) {
        Long count = entityManager
            .createQuery("select count(d) from Dining d where d.status = :status", Long.class)
            .setParameter("status", status)
            .getSingleResult();
        return count == null ? 0L : count;
    }

    public long countActiveByStatus(TableStatus status) {
        Long count = entityManager
            .createQuery("""
                select count(t) from DiningTable t
                where t.deletedAt is null
                  and t.status = :status
                """, Long.class)
            .setParameter("status", status)
            .getSingleResult();
        return count == null ? 0L : count;
    }

    public Map<Long, String> findTableNumberMapByOrderIds(
        Collection<Long> orderIds,
        DiningStatus status
    ) {
        if (orderIds == null || orderIds.isEmpty()) return Map.of();

        List<Object[]> rows = entityManager
            .createQuery("""
                select do.orderId, t.tableNumber
                from DiningOrder do, Dining d, DiningTable t
                where d.id = do.diningId
                  and t.id = d.tableId
                  and t.deletedAt is null
                  and d.status = :status
                  and do.orderId in :orderIds
                """, Object[].class)
            .setParameter("status", status)
            .setParameter("orderIds", orderIds)
            .getResultList();

        Map<Long, String> tableNumbersByOrderId = new LinkedHashMap<>();
        for (Object[] row : rows)
            tableNumbersByOrderId.putIfAbsent((Long) row[0], (String) row[1]);

        return tableNumbersByOrderId;
    }

}
