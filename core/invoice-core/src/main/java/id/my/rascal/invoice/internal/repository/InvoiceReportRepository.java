package id.my.rascal.invoice.internal.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import id.my.rascal.invoice.internal.entity.InvoiceStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

// Read only — agregasi untuk report-core lewat InvoiceReportApi.
@Repository
public class InvoiceReportRepository {

    private static final List<InvoiceStatus> OUTSTANDING =
        List.of(InvoiceStatus.OPEN, InvoiceStatus.PARTIALLY_PAID);

    private final EntityManager entityManager;

    public InvoiceReportRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Fakta pelunasan periode (berdasarkan paid_at / settled_amount yang dibekukan). */
    public record SettlementFacts(long settledInvoices, long settledAmount) {}

    /** Snapshot piutang sekarang (bukan filter periode). */
    public record OutstandingFacts(long outstandingInvoices, long outstandingAmount) {}

    /** Agregasi satu baris menu dari invoice yang dilunasi pada periode. */
    public record MenuSalesAggregate(Long menuId, String description, long qty, long revenue) {}

    /** Status invoice aktif yang memuat satu order. */
    public record OrderBillingStatus(Long orderId, InvoiceStatus status) {}

    public SettlementFacts findSettlementFacts(LocalDateTime from, LocalDateTime to) {
        Tuple row = entityManager
            .createQuery("""
                select count(i) as settledInvoices, coalesce(sum(i.settledAmount), 0) as settledAmount
                from Invoice i
                where i.deletedAt is null
                  and i.paidAt >= :from and i.paidAt < :to
                """, Tuple.class)
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult();
        return new SettlementFacts(numberOf(row, "settledInvoices"), numberOf(row, "settledAmount"));
    }

    public OutstandingFacts findOutstandingFacts() {
        Tuple row = entityManager
            .createQuery("""
                select count(i) as outstandingInvoices, coalesce(sum(i.remainingAmount), 0) as outstandingAmount
                from Invoice i
                where i.deletedAt is null
                  and i.status in :statuses
                  and i.remainingAmount > 0
                """, Tuple.class)
            .setParameter("statuses", OUTSTANDING)
            .getSingleResult();
        return new OutstandingFacts(numberOf(row, "outstandingInvoices"), numberOf(row, "outstandingAmount"));
    }

    /**
     * B16: dibatasi di sisi SQL ({@code order by} + {@code maxResults}) — pemotongan top-N
     * tidak lagi memuat seluruh baris agregasi ke memori sebelum dipotong di Java.
     * Penggabungan baris dengan deskripsi berbeda (menu berganti nama di tengah periode)
     * tetap dilakukan di Java, sekarang hanya atas baris yang sudah terbatas.
     */
    public List<MenuSalesAggregate> findMenuSalesBetween(LocalDateTime from, LocalDateTime to, int limit) {
        return entityManager
            .createQuery("""
                select it.menuId as menuId, it.description as description,
                       sum(it.quantity) as qty, sum(it.amount) as revenue
                from Invoice i join i.items it
                where i.deletedAt is null
                  and i.paidAt >= :from and i.paidAt < :to
                group by it.menuId, it.description
                order by sum(it.amount) desc
                """, Tuple.class)
            .setParameter("from", from)
            .setParameter("to", to)
            .setMaxResults(Math.max(limit, 0))
            .getResultList()
            .stream()
            .map(row -> new MenuSalesAggregate(
                (Long) row.get("menuId"),
                (String) row.get("description"),
                numberOf(row, "qty"),
                numberOf(row, "revenue")))
            .toList();
    }

    public List<OrderBillingStatus> findStatusByOrderIds(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return List.of();

        return entityManager
            .createQuery("""
                select it.orderId as orderId, i.status as status
                from Invoice i join i.items it
                where i.deletedAt is null
                  and it.orderId in :orderIds
                """, Tuple.class)
            .setParameter("orderIds", orderIds)
            .getResultList()
            .stream()
            .map(row -> new OrderBillingStatus(
                (Long) row.get("orderId"),
                (InvoiceStatus) row.get("status")))
            .toList();
    }

    /**
     * B14: ekstraksi fail-loud — kalau bentuk proyeksi berubah tanpa sengaja, error muncul
     * jelas, bukan angka diam-diam menjadi 0 di laporan.
     */
    private static long numberOf(Tuple row, String alias) {
        Object value = row.get(alias);
        if (!(value instanceof Number number))
            throw new IllegalStateException(
                "Unexpected report projection type for '" + alias + "': "
                    + (value == null ? "null" : value.getClass().getName()));
        return number.longValue();
    }

}
