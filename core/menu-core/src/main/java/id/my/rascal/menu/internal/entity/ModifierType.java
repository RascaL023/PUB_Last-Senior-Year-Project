package id.my.rascal.menu.internal.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter @Setter
@Table(name = "menu_modifier_types")
public class ModifierType {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "min_selection", nullable = false, columnDefinition = "int default 1")
    private Integer minSelection;
    
    @Column(name = "max_selection", nullable = false, columnDefinition = "int default 1")
    private Integer maxSelection;

    @OneToMany(
        mappedBy = "modifierType", 
        cascade = CascadeType.ALL, orphanRemoval = true
    ) @BatchSize(size = 50)
    private List<ModifierOption> modifierOptions = new ArrayList<>();

    @ManyToMany(mappedBy = "modifierTypes")
    private List<Menu> menus = new ArrayList<>();
    
}
