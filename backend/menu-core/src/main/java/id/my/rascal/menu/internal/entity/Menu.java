package id.my.rascal.menu.internal.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import id.my.rascal.menu.internal.model.enums.MenuCategoryEnum;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "menus")
public class Menu {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    private MenuCategoryEnum categoryEnum;

    @Column(name = "description")
    private String description;

    @ElementCollection
    private List<String> imageUrls = new ArrayList<>();


    @Column(name = "base_price", nullable = false)
    private Integer basePrice;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToMany
    @JoinTable(
        name = "menu_modifier_types",
        joinColumns = @JoinColumn(name = "menu_id"),
        inverseJoinColumns = @JoinColumn(name = "modifier_type_id")
    ) private List<ModifierType> modifierTypes;

}

