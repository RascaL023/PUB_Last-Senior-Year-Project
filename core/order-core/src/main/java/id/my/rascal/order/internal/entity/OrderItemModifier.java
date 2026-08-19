package id.my.rascal.order.internal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "order_item_modifiers")
public class OrderItemModifier {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "modifier_type_id")
    private Long modifierTypeId;

    @Column(name = "modifier_option_id")
    private Long modifierOptionId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "additional_price", nullable = false)
    private Integer additionalPrice;

}
