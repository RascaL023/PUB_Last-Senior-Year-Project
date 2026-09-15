package id.my.rascal.invoice.internal.repository;

import id.my.rascal.invoice.internal.entity.Invoice;
import id.my.rascal.invoice.internal.entity.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByInvoiceNumber(String invoiceNumber);

    boolean existsByItemsOrderItemId(Long orderItemId);

    @Query("select i from Invoice i where i.deletedAt is null and i.id = :id")
    @EntityGraph(attributePaths = "items")
    Optional<Invoice> findActiveById(@Param("id") Long id);

    @Query("select i from Invoice i where i.deletedAt is null and i.diningId = :diningId")
    @EntityGraph(attributePaths = "items")
    Optional<Invoice> findActiveByDiningId(@Param("diningId") Long diningId);

    @Query("select distinct i from Invoice i join i.items it where i.deletedAt is null and it.orderId = :orderId")
    @EntityGraph(attributePaths = "items")
    List<Invoice> findActiveByItemsOrderId(@Param("orderId") Long orderId);

    @Query("select i from Invoice i where i.deletedAt is null and i.invoiceNumber = :invoiceNumber")
    @EntityGraph(attributePaths = "items")
    Optional<Invoice> findActiveByInvoiceNumber(@Param("invoiceNumber") String invoiceNumber);

    @Query("""
        select distinct i from Invoice i left join i.items it
        where i.deletedAt is null
          and (:keyword is null or
               lower(i.invoiceNumber) like lower(concat('%', cast(:keyword as string), '%')))
          and (:status is null or i.status = :status)
          and (:diningId is null or i.diningId = :diningId)
          and (:orderId is null or it.orderId = :orderId)
        order by i.createdAt desc
    """)
    Page<Invoice> searchActive(
        @Param("keyword") String keyword,
        @Param("status") InvoiceStatus status,
        @Param("diningId") Long diningId,
        @Param("orderId") Long orderId,
        Pageable pageable
    );

}
