package id.my.rascal.menu.internal.service;

import java.util.List;

import org.springframework.stereotype.Component;

import id.my.rascal.image.api.ImageApi;
import id.my.rascal.menu.internal.entity.Menu;
import id.my.rascal.menu.internal.entity.MenuCategory;
import id.my.rascal.menu.internal.entity.ModifierOption;
import id.my.rascal.menu.internal.entity.ModifierType;
import id.my.rascal.menu.internal.model.response.MenuCategoryResponse;
import id.my.rascal.menu.internal.model.response.MenuResponse;
import id.my.rascal.menu.internal.model.response.ModifierOptionResponse;
import id.my.rascal.menu.internal.model.response.ModifierTypeResponse;
import id.my.rascal.menu.internal.model.search.CategoryProjection;
import id.my.rascal.menu.internal.model.search.MenuSearchDocument;
import id.my.rascal.menu.internal.model.search.ModifierOptionProjection;
import id.my.rascal.menu.internal.model.search.ModifierTypeProjection;

@Component
public class MenuResponseMapper {

    private final ImageApi imageService;

    public MenuResponseMapper(ImageApi imageService) {
        this.imageService = imageService;
    }

    public MenuResponse from(Menu menu) {
        return new MenuResponse(
            menu.getId(),
            menu.getName(),
            menu.getDescription(),
            menu.getCategories().stream().map(this::toCategoryResponse).toList(),
            menu.getImageUrls().stream().map(imageService::resolveUrl).toList(),
            menu.getBasePrice(),
            menu.getIsAvailable(),
            menu.getCreatedAt(),
            menu.getUpdatedAt(),
            menu.getModifierTypes().stream().map(this::toModifierTypeResponse).toList(),
            menu.getDeletedAt()
        );
    }

    public MenuResponse from(MenuSearchDocument doc) {
        return new MenuResponse(
            doc.getId(),
            doc.getName(),
            doc.getDescription(),
            safeList(doc.getCategories()).stream().map(this::toCategoryResponse).toList(),
            safeList(doc.getImageUrls()),
            doc.getBasePrice(),
            doc.getIsAvailable(),
            doc.getCreatedAt(),
            doc.getUpdatedAt(),
            safeList(doc.getModifierTypes()).stream().map(this::toModifierTypeResponse).toList(),
            doc.getDeletedAt()
        );
    }


    private MenuCategoryResponse toCategoryResponse(MenuCategory category) {
        return new MenuCategoryResponse(
            category.getId(),
            category.getDisplayName(),
            category.getCategoryCode(),
            category.getDisplayOrder()
        );
    }

    private ModifierTypeResponse toModifierTypeResponse(ModifierType modifierType) {
        return new ModifierTypeResponse(
            modifierType.getId(),
            modifierType.getName(),
            modifierType.getMinSelection(),
            modifierType.getMaxSelection(),
            modifierType.getModifierOptions().stream()
                .map(this::toOptionResponse).toList()
        );
    }

    private ModifierOptionResponse toOptionResponse(ModifierOption option) {
        return new ModifierOptionResponse(
            option.getId(),
            option.getName(),
            option.getAdditionalPrice()
        );
    }

    // ---- projection helpers ----

    private MenuCategoryResponse toCategoryResponse(CategoryProjection category) {
        return new MenuCategoryResponse(
            category.id(),
            category.name(),
            category.categoryCode(),
            category.displayOrder()
        );
    }

    private ModifierTypeResponse toModifierTypeResponse(ModifierTypeProjection modifierType) {
        return new ModifierTypeResponse(
            modifierType.id(),
            modifierType.name(),
            modifierType.minSelection(),
            modifierType.maxSelection(),
            safeList(modifierType.options()).stream()
                .map(this::toOptionResponse).toList()
        );
    }

    private ModifierOptionResponse toOptionResponse(ModifierOptionProjection option) {
        return new ModifierOptionResponse(
            option.id(),
            option.name(),
            option.additionalPrice()
        );
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

}
