package id.my.rascal.order.internal.repository;

import id.my.rascal.order.internal.entity.Order;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByOrderNumber(String orderNumber);

    @Query("select o from Order o where o.deletedAt is null and o.id = :id")
    @EntityGraph(attributePaths = "orderItems")
    Optional<Order> findActiveById(@Param("id") Long id);

    @Query("""
        select o from Order o
        where o.deletedAt is null
          and (:keyword is null or
               lower(o.orderNumber) like lower(concat('%', cast(:keyword as string), '%'))
               or (o.customerName is not null and lower(o.customerName) like lower(concat('%', cast(:keyword as string), '%'))))
          and (:status is null or o.status = :status)
          and (:paymentStatus is null or o.paymentStatus = :paymentStatus)
        order by o.createdAt desc
    """)
    Page<Order> searchActive(
        @Param("keyword") String keyword,
        @Param("status") OrderStatus status,
        @Param("paymentStatus") PaymentStatus paymentStatus,
        Pageable pageable
    );

}
