package id.my.rascal.report.internal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import id.my.rascal.report.internal.entity.ReportProcessedEvent;

@Repository
public interface ReportProcessedEventRepository extends JpaRepository<ReportProcessedEvent, Long> {

    boolean existsByEventTypeAndEventKey(String eventType, String eventKey);

}
