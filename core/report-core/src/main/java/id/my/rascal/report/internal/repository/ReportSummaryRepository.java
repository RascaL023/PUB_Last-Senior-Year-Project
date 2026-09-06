package id.my.rascal.report.internal.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;

// Read!
@Repository
public class ReportSummaryRepository {

    private final EntityManager entityManager;

    public ReportSummaryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long sumRevenueBetween(LocalDate fromInclusive, LocalDate toInclusive) {
        Long sum = entityManager
            .createQuery("""
                select coalesce(sum(r.revenue), 0) from ReportDailySales r
                where r.saleDate >= :from and r.saleDate <= :to
                """, Long.class)
            .setParameter("from", fromInclusive)
            .setParameter("to", toInclusive)
            .getSingleResult();
        return sum == null ? 0L : sum;
    }

    public List<TopMenuRow> findTopMenusBetween(LocalDate fromInclusive, LocalDate toInclusive, int limit) {
        List<Object[]> rows = entityManager
            .createQuery("""
                select max(r.menuId), r.menuName, sum(r.qty), sum(r.revenue)
                from ReportMenuDaily r
                where r.saleDate >= :from and r.saleDate <= :to
                group by r.menuName
                order by sum(r.revenue) desc
                """, Object[].class)
            .setParameter("from", fromInclusive)
            .setParameter("to", toInclusive)
            .setMaxResults(limit)
            .getResultList();

        return rows.stream()
            .map(row -> new TopMenuRow(
                (Long) row[0],
                (String) row[1],
                (Long) row[2],
                (Long) row[3]
            ))
            .toList();
    }

    public record TopMenuRow(
        Long menuId,
        String menuName,
        Long qty,
        Long revenue
    ) {}

}
