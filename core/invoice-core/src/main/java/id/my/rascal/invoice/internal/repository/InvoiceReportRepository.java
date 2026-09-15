package id.my.rascal.invoice.internal.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import id.my.rascal.invoice.internal.entity.InvoiceStatus;
import jakarta.persistence.EntityManager;

// Read only — agregasi untuk report-core lewat InvoiceReportApi.
@Repository
public class InvoiceReportRepository {

    private static final List<InvoiceStatus> OUTSTANDING =
        List.of(InvoiceStatus.OPEN, InvoiceStatus.PARTIALLY_PAID);

    private final EntityManager entityManager;

    public InvoiceReportRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** [0] = jumlah invoice yang dilunasi pada periode, [1] = Σ settled_amount. */
    public Object[] findSettlementFacts(LocalDateTime from, LocalDateTime to) {
        return entityManager
            .createQuery("""
                select count(i), coalesce(sum(i.settledAmount), 0) from Invoice i
                where i.deletedAt is null
                  and i.paidAt >= :from and i.paidAt < :to
                """, Object[].class)
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult();
    }

    /** Σ koreksi tagihan (refund baris invoice) pada periode. */
    public long sumRefundedBetween(LocalDateTime from, LocalDateTime to) {
        Number sum = entityManager
            .createQuery("""
                select coalesce(sum(r.scopeAmount), 0) from Refund r
                where r.createdAt >= :from and r.createdAt < :to
                """, Number.class)
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult();
        return sum == null ? 0L : sum.longValue();
    }

    /** [0] = jumlah invoice belum lunas sekarang, [1] = Σ remaining_amount. */
    public Object[] findOutstandingFacts() {
        return entityManager
            .createQuery("""
                select count(i), coalesce(sum(i.remainingAmount), 0) from Invoice i
                where i.deletedAt is null
                  and i.status in :statuses
                  and i.remainingAmount > 0
                """, Object[].class)
            .setParameter("statuses", OUTSTANDING)
            .getSingleResult();
    }

    /** Baris invoice dari invoice yang dilunasi pada periode, dikelompokkan per menu. */
    public List<Object[]> findMenuSalesBetween(LocalDateTime from, LocalDateTime to) {
        return entityManager
            .createQuery("""
                select it.menuId, it.description, sum(it.quantity), sum(it.amount)
                from Invoice i join i.items it
                where i.deletedAt is null
                  and i.paidAt >= :from and i.paidAt < :to
                group by it.menuId, it.description
                """, Object[].class)
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();
    }

    public List<Object[]> findStatusByOrderIds(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return List.of();

        return entityManager
            .createQuery("""
                select it.orderId, i.status
                from Invoice i join i.items it
                where i.deletedAt is null
                  and it.orderId in :orderIds
                """, Object[].class)
            .setParameter("orderIds", orderIds)
            .getResultList();
    }

}
