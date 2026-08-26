package id.my.rascal.menu.internal.entity;

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
@Table(name = "menu_modifier_options")
public class ModifierOption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "additional_price", nullable = false)
    private Integer additionalPrice;
    
    @ManyToOne
    @JoinColumn(name = "modifier_type_id")
    private ModifierType modifierType;
    
}
