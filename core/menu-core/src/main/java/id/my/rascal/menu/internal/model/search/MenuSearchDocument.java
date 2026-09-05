package id.my.rascal.menu.internal.model.search;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class MenuSearchDocument {

    private Long id;
    private String name;
    private String description;
    private Integer basePrice;
    private Boolean isAvailable;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> imageUrls;
    private List<Long> categoryIds;
    private List<CategoryProjection> categories;
    private List<ModifierTypeProjection> modifierTypes;

}
