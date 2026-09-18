package id.my.rascal.dining.api;

import java.util.Collection;
import java.util.Map;

public interface DiningReportApi {

    long countOpenDinings();
    long countOccupiedTables();
    long countAvailableTables();
    Map<Long, String> tableNumbersByOrderIds(Collection<Long> orderIds);

}
