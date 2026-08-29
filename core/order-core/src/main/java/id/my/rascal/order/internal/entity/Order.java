package id.my.rascal.order.internal.entity;

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

import id.my.rascal.order.internal.model.enums.OrderPaidStatus;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.enums.OrderType;

@Entity
@Getter @Setter
@Table(name = "orders")
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "paid_status", nullable = false)
    private OrderPaidStatus paidStatus;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "notes")
    private String notes;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    ) @BatchSize(size = 50)
    private List<OrderItem> orderItems = new ArrayList<>();


    public void markCreated() {
        this.status = OrderStatus.CREATED;
    }

    public void markConfirmed() {
        this.status = OrderStatus.CONFIRMED;
    }

    public void markPreparing() {
        this.status = OrderStatus.PREPARING;
    }

    public void markReady() {
        this.status = OrderStatus.READY;
    }

    public void markCompleted() {
        this.status = OrderStatus.COMPLETED;
    }

    public void markCancelled() {
        this.status = OrderStatus.CANCELLED;
    }

    public void markUnpaid() {
        this.paidStatus = OrderPaidStatus.UNAPAID;
    }

    public void markPaid() {
        this.paidStatus = OrderPaidStatus.PAID;
    }

}
