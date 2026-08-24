package id.my.rascal.payment.internal.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;
import id.my.rascal.payment.internal.model.enums.PaymentTargetType;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByExternalId(String externalId);

    @Query("select p from Payment p where p.deletedAt is null and p.id = :id")
    Optional<Payment> findActiveById(@Param("id") Long id);

    @Query("""
        select p from Payment p
        where p.deletedAt is null
          and (:keyword is null or
               lower(p.targetReference) like lower(concat('%', cast(:keyword as string), '%'))
               or (p.paymentDetail is not null and lower(p.paymentDetail) like lower(concat('%', cast(:keyword as string), '%'))))
          and (:targetType is null or p.targetType = :targetType)
          and (:targetId is null or p.targetId = :targetId)
          and (:status is null or p.status = :status)
          and (:paymentMethodId is null or p.paymentMethodId = :paymentMethodId)
        order by p.createdAt desc
    """)
    Page<Payment> searchActive(
        @Param("keyword") String keyword,
        @Param("targetType") PaymentTargetType targetType,
        @Param("targetId") Long targetId,
        @Param("status") PaymentStatus status,
        @Param("paymentMethodId") Long paymentMethodId,
        Pageable pageable
    );

}
