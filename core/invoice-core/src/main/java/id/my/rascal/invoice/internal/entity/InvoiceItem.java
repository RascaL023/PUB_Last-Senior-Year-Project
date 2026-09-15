package id.my.rascal.invoice.internal.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(
    name = "invoice_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_invoice_items_invoice_order_item",
        columnNames = {"invoice_id", "order_item_id"}
    )
)
public class InvoiceItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "order_id")
    private Long orderId;

    // Snapshot menu asal baris ini — dipakai report untuk proyeksi menu harian.
    // Bisa null untuk baris invoice manual yang tidak berasal dari order item.
    @Column(name = "menu_id")
    private Long menuId;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "refunded", nullable = false)
    private Boolean refunded = false;

}
