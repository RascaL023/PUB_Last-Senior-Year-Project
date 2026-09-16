package id.my.rascal.payment.internal.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import id.my.rascal.payment.internal.model.enums.PaymentProvider;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;

@Entity
@Getter @Setter
@Table(name = "payments")
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "applied_amount", nullable = false)
    private Integer appliedAmount = 0;

    @Column(name = "excess_amount", nullable = false)
    private Integer excessAmount = 0;


    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider", nullable = false)
    private PaymentProvider paymentProvider;

    @Column(name = "payment_method")
    private String paymentMethodName;

    @Column(name = "payment_channel")
    private String paymentChannel;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "invoice_url")
    private String invoiceUrl;

    @Column(name = "payment_detail")
    private String paymentDetail;

    @Lob @Column(name = "raw_webhook")
    private String rawWebhook;

    @Column(name = "invoice_number")
    private String invoiceNumber;


    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

}
