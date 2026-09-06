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
    name = "report_menu_daily",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_report_menu_daily_date_menu",
        columnNames = {"sale_date", "menu_name"}
    )
)
public class ReportMenuDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(name = "menu_id")
    private Long menuId;

    @Column(name = "menu_name", nullable = false)
    private String menuName;

    @Column(name = "qty", nullable = false)
    private Long qty;

    @Column(name = "revenue", nullable = false)
    private Long revenue;

    public ReportMenuDaily() {}

    public ReportMenuDaily(LocalDate saleDate, Long menuId, String menuName, Long qty, Long revenue) {
        this.saleDate = saleDate;
        this.menuId = menuId;
        this.menuName = menuName;
        this.qty = qty;
        this.revenue = revenue;
    }

}
