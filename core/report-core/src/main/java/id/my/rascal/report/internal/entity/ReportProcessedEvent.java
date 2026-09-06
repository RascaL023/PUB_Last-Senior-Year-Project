package id.my.rascal.report.internal.entity;

import java.time.LocalDateTime;

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
    name = "report_processed_event",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_report_processed_event",
        columnNames = {"event_type", "event_key"}
    )
)
public class ReportProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_key", nullable = false)
    private String eventKey;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public ReportProcessedEvent() {}

    public ReportProcessedEvent(String eventType, String eventKey, LocalDateTime processedAt) {
        this.eventType = eventType;
        this.eventKey = eventKey;
        this.processedAt = processedAt;
    }

}
