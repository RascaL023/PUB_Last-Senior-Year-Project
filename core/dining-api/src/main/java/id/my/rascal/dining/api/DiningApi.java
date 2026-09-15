package id.my.rascal.dining.api;

import java.util.Collection;
import java.util.List;

public interface DiningApi {

    DiningApiResponse getDining(Long id);

    List<DiningApiResponse> getDinings(Collection<Long> ids);

    List<Long> getOrderIds(Long diningId);

    /** Temporary [cycle spring wiring depencency on it's implementation] */
    String getDiningStatus(Long diningId);

}
