package id.my.rascal.order.internal.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import id.my.rascal.order.internal.entity.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
        select i from OrderItem i
        where i.order.deletedAt is null
          and i.order.id in :orderIds
        order by i.id asc
    """)
    List<OrderItem> findActiveByOrderIds(@Param("orderIds") Collection<Long> orderIds);

}
