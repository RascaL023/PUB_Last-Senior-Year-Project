package id.my.rascal.menu.internal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.image.ImageService;
import id.my.rascal.menu.internal.entity.Menu;
import id.my.rascal.menu.internal.entity.MenuCategory;
import id.my.rascal.menu.internal.entity.ModifierOption;
import id.my.rascal.menu.internal.entity.ModifierType;
import id.my.rascal.menu.internal.model.request.MenuPutRequest;
import id.my.rascal.menu.internal.model.request.MenuRequest;
import id.my.rascal.menu.internal.model.response.MenuCategoryResponse;
import id.my.rascal.menu.internal.model.response.MenuResponse;
import id.my.rascal.menu.internal.model.response.ModifierOptionResponse;
import id.my.rascal.menu.internal.model.response.ModifierTypeResponse;

@Service
public class MenuV1ApplicationService {

    private final MenuService menuService;
    private final ImageService imageService;

    public MenuV1ApplicationService(MenuService menuService, ImageService imageService) {
        this.menuService = menuService;
        this.imageService = imageService;
    }

    @Transactional
    public MenuResponse create(MenuRequest request) {
        return toResponse(menuService.create(request));
    }

    @Transactional(readOnly = true)
    public MenuResponse getById(Long id) {
        return toResponse(menuService.getById(id));
    }

    @Transactional(readOnly = true)
    public Page<MenuResponse> getAllPaged(String name, Long categoryId, Pageable pageable) {
        return menuService.getAllPaged(name, categoryId, pageable).map(this::toResponse);
    }

    @Transactional
    public MenuResponse update(Long id, MenuPutRequest request) {
        return toResponse(menuService.update(id, request));
    }

    @Transactional
    public MenuResponse restore(Long id) {
        return toResponse(menuService.restore(id));
    }

    @Transactional
    public void delete(Long id) {
        menuService.delete(id);
    }


    private MenuResponse toResponse(Menu menu) {
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
            menu.getModifierTypes().stream().map(this::toModifierTypeResponse).toList()
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

}