package id.my.rascal.report.internal.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import id.my.rascal.report.internal.entity.ReportMenuDaily;

@Repository
public interface ReportMenuDailyRepository extends JpaRepository<ReportMenuDaily, Long> {

    Optional<ReportMenuDaily> findBySaleDateAndMenuName(LocalDate saleDate, String menuName);

    @Modifying
    @Query("""
        update ReportMenuDaily r
        set r.qty = r.qty + :qty, r.revenue = r.revenue + :revenue
        where r.saleDate = :saleDate and r.menuName = :menuName
        """)
    int addQtyAndRevenue(
        @Param("saleDate") LocalDate saleDate,
        @Param("menuName") String menuName,
        @Param("qty") long qty,
        @Param("revenue") long revenue
    );

}
