package id.my.rascal.invoice.internal.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import id.my.rascal.invoice.internal.entity.Refund;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByInvoiceId(Long invoiceId);
}
