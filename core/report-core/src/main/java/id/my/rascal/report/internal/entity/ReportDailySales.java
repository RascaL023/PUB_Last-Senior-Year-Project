package id.my.rascal.report.internal.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(
    name = "report_daily_sales",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_report_daily_sales_date",
        columnNames = "sale_date"
    )
)
public class ReportDailySales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(name = "revenue", nullable = false)
    private Long revenue;

    public ReportDailySales() {}

    public ReportDailySales(LocalDate saleDate, Long revenue) {
        this.saleDate = saleDate;
        this.revenue = revenue;
    }

}
