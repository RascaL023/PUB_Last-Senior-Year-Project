package id.my.rascal.payment.internal.repository;

import java.time.LocalDateTime;

import org.springframework.stereotype.Repository;

import id.my.rascal.payment.internal.model.enums.PaymentStatus;
import id.my.rascal.payment.internal.model.enums.PaymentTargetType;
import jakarta.persistence.EntityManager;

@Repository
public class PaymentReportRepository {

    private final EntityManager entityManager;

    public PaymentReportRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long sumAmountByStatusInPeriod(LocalDateTime from, LocalDateTime to) {
        Long sum = entityManager
            .createQuery("""
                select coalesce(sum(p.amount), 0) from Payment p
                where p.status = :status
                  and p.deletedAt is null
                  and p.paidAt >= :from and p.paidAt < :to
                """, Long.class)
            .setParameter("status", PaymentStatus.PAID)
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult();
        return sum == null ? 0L : sum;
    }

    public long countDistinctTargetsByStatusAndTargetTypeInPeriod(LocalDateTime from, LocalDateTime to) {
        Long count = entityManager
            .createQuery("""
                select count(distinct p.targetId) from Payment p
                where p.status = :status
                  and p.targetType = :targetType
                  and p.deletedAt is null
                  and p.paidAt >= :from and p.paidAt < :to
                """, Long.class)
            .setParameter("status", PaymentStatus.PAID)
            .setParameter("targetType", PaymentTargetType.ORDER)
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult();
        return count == null ? 0L : count;
    }

}
