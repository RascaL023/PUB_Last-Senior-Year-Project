package id.my.rascal.payment.internal.repository;

import java.time.LocalDateTime;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;

// Read only — agregasi untuk report-core lewat PaymentReportApi.
@Repository
public class PaymentReportRepository {

    private final EntityManager entityManager;

    public PaymentReportRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long sumAppliedSettledBetween(LocalDateTime from, LocalDateTime to) {
        Number sum = entityManager
            .createQuery("""
                select coalesce(sum(p.appliedAmount), 0) from Payment p
                where p.paidAt >= :from and p.paidAt < :to
                """, Number.class)
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult();
        return sum == null ? 0L : sum.longValue();
    }

    public long sumRefundedBetween(LocalDateTime from, LocalDateTime to) {
        Number sum = entityManager
            .createQuery("""
                select coalesce(sum(p.amount), 0) from Payment p
                where p.refundedAt >= :from and p.refundedAt < :to
                """, Number.class)
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult();
        return sum == null ? 0L : sum.longValue();
    }

}
