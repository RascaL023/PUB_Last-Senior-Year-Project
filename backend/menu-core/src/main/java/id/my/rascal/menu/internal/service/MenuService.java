package id.my.rascal.menu.internal.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.common.image.ImageService;
import id.my.rascal.common.util.StringUtil;
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
import id.my.rascal.menu.internal.repository.MenuRepository;

@Service
public class MenuService {

    private final MenuRepository menuRepository;
    private final ModifierHelper modifierHelper;
    private final MenuCategoryHelper menuCategoryHelper;
    private final ImageService imageService;

    public MenuService(
        MenuRepository menuRepository,
        ModifierHelper modifierHelper,
        MenuCategoryHelper menuCategoryHelper,
        ImageService imageService
    ) {
        this.menuRepository = menuRepository;
        this.modifierHelper = modifierHelper;
        this.menuCategoryHelper = menuCategoryHelper;
        this.imageService = imageService;
    }

    @Transactional
    public MenuResponse create(MenuRequest request) {
        List<ModifierType> modifierTypes = modifierHelper.getByIds(request.ModifierTypeIds());
        validateModifierTypes(modifierTypes, request.ModifierTypeIds());

        List<MenuCategory> categories = menuCategoryHelper.getActiveByIds(request.categoryIds());
        validateCategories(categories, request.categoryIds());

        Menu menu = new Menu();
        menu.setCategories(new ArrayList<>(categories));
        menu.setModifierTypes(new ArrayList<>(modifierTypes));

        fill(
            menu, request.name(), request.description(),
            request.imageUrls(), request.basePrice(),
            request.isAvailable() == null ? Boolean.FALSE : request.isAvailable()
        );

        menu.setCreatedAt(LocalDateTime.now());
        return toResponse(menuRepository.save(menu));
    }

    @Transactional(readOnly = true)
    public MenuResponse getById(Long id) {
        Menu menu = menuRepository.findWithRelationsById(id, false)
            .orElseThrow(() -> new NotFoundException("Menu with id " + id + " not found"));

        return toResponse(menu);
    }

    @Transactional(readOnly = true)
    public Page<MenuResponse> getAllPaged(String name, Long categoryId, Pageable pageable) {
        name = StringUtil.normalizeSearch(name);

        Page<Long> idPage = menuRepository.findSearchIds(name, categoryId, false, pageable);
        if (idPage.getContent().isEmpty())
            return Page.empty(pageable);

        List<Menu> menus = menuRepository.findAllWithRelationsByIds(idPage.getContent());
        if (!idPage.getContent().isEmpty()) {
            List<Menu> ordered = new ArrayList<>();
            for (Long id : idPage.getContent()) {
                menus.stream()
                    .filter(m -> m.getId().equals(id))
                    .findFirst()
                    .ifPresent(ordered::add);
            }
            menus = ordered;
        }

        List<MenuResponse> content = menus.stream().map(this::toResponse).toList();

        return new PageImpl<>(content, pageable, idPage.getTotalElements());
    }

    @Transactional
    public MenuResponse update(Long id, MenuPutRequest request) {
        Menu menu = menuRepository.findWithRelationsById(id, false)
            .orElseThrow(() -> new NotFoundException("Menu with id " + id + " not found"));

        List<ModifierType> modifierTypes = modifierHelper.getByIds(request.ModifierTypeIds());
        validateModifierTypes(modifierTypes, request.ModifierTypeIds());

        List<MenuCategory> categories = menuCategoryHelper.getActiveByIds(request.categoryIds());
        validateCategories(categories, request.categoryIds());

        menu.getCategories().clear();
        menu.getCategories().addAll(categories);

        menu.getModifierTypes().clear();
        menu.getModifierTypes().addAll(modifierTypes);

        fill(
            menu, request.name(), request.description(),
            request.imageUrls(), request.basePrice(),
            request.isAvailable() == null ? Boolean.FALSE : request.isAvailable()
        );

        menu.setUpdatedAt(LocalDateTime.now());
        return toResponse(menuRepository.save(menu));
    }

    @Transactional
    public MenuResponse restore(Long id) {
        Menu menu = menuRepository.findWithRelationsById(id, true)
            .orElseThrow(() -> new NotFoundException("Menu with id " + id + " not found or not deleted"));
        
        menu.setDeletedAt(null);
        menu.setUpdatedAt(LocalDateTime.now());
        return toResponse(menuRepository.save(menu));
    }

    @Transactional
    public void delete(Long id) {
        Menu menu = menuRepository.findWithRelationsById(id, false)
            .orElseThrow(() -> new NotFoundException("Menu with id " + id + " not found"));

        menu.setDeletedAt(LocalDateTime.now());
        menuRepository.save(menu);
    }


    private Menu fill(
        Menu menu,
        String name,
        String description,
        List<String> imageUrls,
        Integer basePrice,
        Boolean isAvailable
    ) {
        String normalizedName = StringUtil.normalizeSpaces(name);
        normalizedName = StringUtil.capitalize(normalizedName);

        String normalizedDescription = StringUtil.normalizeSpaces(description);
        normalizedDescription = StringUtil.capitalize(normalizedDescription);

        List<String> normalizedImageUrls = imageUrls == null
            ? List.of()
            : imageUrls.stream()
                .map(path -> StringUtil.safeIsBlank(path) ? 
                    null : StringUtil.normalizeSpaces(path).trim())
                .filter(Objects::nonNull)
                .toList();

        menu.setName(normalizedName);
        menu.setDescription(normalizedDescription);
        menu.setImageUrls(new ArrayList<>(normalizedImageUrls));
        menu.setBasePrice(basePrice);
        menu.setIsAvailable(isAvailable);

        return menu;
    }

    private void validateModifierTypes(
        List<ModifierType> modifierTypes,
        Collection<Long> requestIds
    ) {
        if (modifierTypes.size() != requestIds.size()) {
            Set<Long> foundIds = modifierTypes.stream()
                .map(ModifierType::getId).collect(Collectors.toSet());

            Set<Long> missingIds = new HashSet<>(requestIds);
            missingIds.removeAll(foundIds);

            throw new NotFoundException("Not found modifier IDs: " + missingIds);
        }
    }

    private void validateCategories(
        List<MenuCategory> categories,
        Collection<Long> requestIds
    ) {
        if (categories.size() != requestIds.size()) {
            Set<Long> foundIds = categories.stream()
                .map(MenuCategory::getId).collect(Collectors.toSet());

            Set<Long> missingIds = new HashSet<>(requestIds);
            missingIds.removeAll(foundIds);

            throw new NotFoundException("Not found category IDs: " + missingIds);
        }
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
