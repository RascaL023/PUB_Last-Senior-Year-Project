package id.my.rascal.dining.internal.adapter;

import java.util.Collection;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.dining.api.DiningReportApi;
import id.my.rascal.dining.internal.entity.DiningStatus;
import id.my.rascal.dining.internal.entity.TableStatus;
import id.my.rascal.dining.internal.repository.DiningReportRepository;

@Component
public class DiningReportApiImpl implements DiningReportApi {

    private final DiningReportRepository diningReportRepository;

    public DiningReportApiImpl(DiningReportRepository diningReportRepository) {
        this.diningReportRepository = diningReportRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public long countOpenDinings() {
        return diningReportRepository.countByStatus(DiningStatus.OPEN);
    }

    @Override
    @Transactional(readOnly = true)
    public long countOccupiedTables() {
        return diningReportRepository.countActiveByStatus(TableStatus.OCCUPIED);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAvailableTables() {
        return diningReportRepository.countActiveByStatus(TableStatus.AVAILABLE);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> tableNumbersByOrderIds(Collection<Long> orderIds) {
        return diningReportRepository.findTableNumberMapByOrderIds(orderIds, DiningStatus.OPEN);
    }

}