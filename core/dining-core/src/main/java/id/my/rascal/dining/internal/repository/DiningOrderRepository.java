package id.my.rascal.dining.internal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import id.my.rascal.dining.internal.entity.DiningOrder;

@Repository
public interface DiningOrderRepository extends JpaRepository<DiningOrder, Long> {

    @Query("select do.orderId from DiningOrder do where do.diningId = :diningId")
    List<Long> findOrderIdsByDiningId(@Param("diningId") Long diningId);

    @Query("select do from DiningOrder do where do.diningId in :diningIds")
    List<DiningOrder> findAllByDiningIds(@Param("diningIds") Collection<Long> diningIds);

    default Map<Long, List<Long>> findOrderIdsGroupedByDiningId(Collection<Long> diningIds) {
        if (diningIds == null || diningIds.isEmpty())
            return Map.of();

        return findAllByDiningIds(diningIds).stream()
            .collect(Collectors.groupingBy(
                DiningOrder::getDiningId,
                Collectors.mapping(DiningOrder::getOrderId, Collectors.toList())
            ));
    }

}
