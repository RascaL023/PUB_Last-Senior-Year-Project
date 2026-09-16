package id.my.rascal.payment.internal.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.model.enums.PaymentProvider;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByExternalId(String externalId);

    Optional<Payment> findByExternalId(String externalId);

    @Query("select p from Payment p where p.deletedAt is null and p.id = :id")
    Optional<Payment> findActiveById(@Param("id") Long id);

    /**
     * Satu payment PENDING aktif per invoice: mencegah dua QRIS/VA menagih tagihan yang sama
     * sekaligus (settle ganda masuk jalur replay-skip dan menghasilkan applied_amount = 0).
     */
    @Query("""
        select count(p) > 0 from Payment p
        where p.deletedAt is null
          and p.invoiceId = :invoiceId
          and p.status = id.my.rascal.payment.internal.model.enums.PaymentStatus.PENDING
        """)
    boolean existsActivePendingByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query("""
        select p from Payment p
        where p.deletedAt is null
          and (:keyword is null or
               lower(p.invoiceNumber) like lower(concat('%', cast(:keyword as string), '%'))
               or (p.paymentDetail is not null and lower(p.paymentDetail) like lower(concat('%', cast(:keyword as string), '%'))))
          and (:invoiceId is null or p.invoiceId = :invoiceId)
          and (:status is null or p.status = :status)
          and (:paymentProvider is null or p.paymentProvider = :paymentProvider)
        order by p.createdAt desc
    """)
    Page<Payment> searchActive(
        @Param("keyword") String keyword,
        @Param("invoiceId") Long invoiceId,
        @Param("status") PaymentStatus status,
        @Param("paymentProvider") PaymentProvider paymentProvider,
        Pageable pageable
    );

}
