package id.my.rascal.invoice.internal.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import id.my.rascal.common.exception.BadRequestException;

@Entity
@Getter @Setter
@Table(name = "invoices")
public class Invoice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "dining_id", unique = true)
    private Long diningId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "paid_amount", nullable = false)
    private Integer paidAmount;

    @Column(name = "remaining_amount", nullable = false)
    private Integer remainingAmount;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(
        mappedBy = "invoice",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    ) @BatchSize(size = 50)
    private List<InvoiceItem> items = new ArrayList<>();


    public void markOpen() {
        this.status = InvoiceStatus.OPEN;
    }

    public void markPartiallyPaid() {
        this.status = InvoiceStatus.PARTIALLY_PAID;
    }

    public void markPaid() {
        this.status = InvoiceStatus.PAID;
    }

    public void markVoid() {
        this.status = InvoiceStatus.VOID;
    }

    public void applyPayment(Integer amount) {
        if (amount == null || amount <= 0)
            throw new BadRequestException("Payment amount must be greater than 0");
        if (this.status == InvoiceStatus.VOID)
            throw new BadRequestException("Cannot apply payment to a VOID invoice");
        if (this.status == InvoiceStatus.PAID)
            throw new BadRequestException("Invoice already paid");
        if (amount > this.remainingAmount)
            throw new BadRequestException("Payment amount exceeds remaining amount");

        this.paidAmount += amount;
        this.remainingAmount = this.totalAmount - this.paidAmount;
        if (this.remainingAmount == 0) markPaid();
        else markPartiallyPaid();
        this.updatedAt = LocalDateTime.now();
    }

    public void voidInvoice() {
        if (this.status == InvoiceStatus.VOID)
            throw new BadRequestException("Invoice already void");
        if (this.status == InvoiceStatus.PAID)
            throw new BadRequestException("Cannot void a PAID invoice");

        markVoid();
        this.updatedAt = LocalDateTime.now();
    }

}
