package id.my.rascal.dining.internal.repository;

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

}
