package id.my.rascal.report.internal.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import id.my.rascal.report.internal.entity.ReportDailySales;

@Repository
public interface ReportDailySalesRepository extends JpaRepository<ReportDailySales, Long> {

    Optional<ReportDailySales> findBySaleDate(LocalDate saleDate);

    @Modifying
    @Query("""
        update ReportDailySales r 
        set r.revenue = r.revenue + :delta 
        where r.saleDate = :saleDate
    """)
    int addRevenue(@Param("saleDate") LocalDate saleDate, @Param("delta") long delta);

}
