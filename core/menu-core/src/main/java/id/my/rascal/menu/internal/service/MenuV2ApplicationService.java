package id.my.rascal.menu.internal.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.menu.internal.entity.Menu;
import id.my.rascal.menu.internal.entity.MenuCategory;
import id.my.rascal.menu.internal.entity.ModifierType;
import id.my.rascal.menu.internal.model.request.MenuPutRequest;
import id.my.rascal.menu.internal.model.request.MenuRequest;
import id.my.rascal.menu.internal.model.response.MenuResponseCached;
import id.my.rascal.menu.internal.model.search.MenuSearchDocument;
import id.my.rascal.menu.internal.repository.MenuIdView;
import id.my.rascal.menu.internal.repository.MenuRepository;

@Service
public class MenuV2ApplicationService {

    private final MenuService menuService;
    private final MenuSearchService menuSearchService;
    private final MenuRepository menuRepository;

    public MenuV2ApplicationService(
        MenuService menuService,
        MenuSearchService menuSearchService,
        MenuRepository menuRepository
    ) {
        this.menuService = menuService;
        this.menuSearchService = menuSearchService;
        this.menuRepository = menuRepository;
    }

    @Transactional
    public MenuResponseCached create(MenuRequest menuRequest) {
        return toResponse(menuService.create(menuRequest));
    }

    @Transactional(readOnly = true)
    public MenuResponseCached getById(Long id) {
        List<MenuIdView> menu = menuService.getByIdCached(id);
        return toResponse(id, menu);
    }
    
    @Transactional(readOnly = true)
    public Page<MenuResponseCached> getAllPaged(
        String name,
        Long categoryId,
        Pageable pageable
    ) {
        // Use Meilisearch with PostgreSQL fallback for ID resolution
        Page<MenuSearchDocument> searchResults = menuSearchService.searchPaged(name, categoryId, pageable);

        List<Long> menuIds = searchResults.getContent().stream()
            .map(MenuSearchDocument::getId)
            .toList();

        if (menuIds.isEmpty())
            return Page.empty(pageable);

        // Resolve cached views using the ordered IDs from search
        List<MenuIdView> rows = menuRepository.findViewByIds(menuIds, false);
        Map<Long, List<MenuIdView>> grouped = rows.stream()
            .collect(Collectors.groupingBy(MenuIdView::getMenuId));

        List<MenuResponseCached> content = menuIds.stream()
            .map(id -> grouped.get(id))
            .filter(Objects::nonNull)
            .map(this::toResponse)
            .toList();

        return new PageImpl<>(content, pageable, searchResults.getTotalElements());
    }

    @Transactional
    public MenuResponseCached update(Long id, MenuPutRequest menuPutRequest) {
        return toResponse(menuService.update(id, menuPutRequest));
    }

    @Transactional
    public MenuResponseCached restore(Long id) {
        return toResponse(menuService.restore(id));
    }

    @Transactional
    public void delete(Long id) {
        menuService.delete(id);
    }


    private MenuResponseCached toResponse(List<MenuIdView> menuIdViews) {
        return toResponse(menuIdViews.getFirst().getMenuId(), menuIdViews);
    }


    private MenuResponseCached toResponse(Long id, List<MenuIdView> menuIdView) {
        Set<Long> categoryIds = menuIdView.stream().map(MenuIdView::getCategoryId)
            .filter(Objects::nonNull).collect(Collectors.toSet());

        Set<Long> modifierTypeIds = menuIdView.stream().map(MenuIdView::getModifierTypeId)
            .filter(Objects::nonNull).collect(Collectors.toSet());

        return new MenuResponseCached(id, categoryIds, modifierTypeIds);
    }

    private MenuResponseCached toResponse(Menu menu) {
        Long id = menu.getId();

        Set<Long> categoryIds = menu.getCategories().stream().map(MenuCategory::getId)
            .filter(Objects::nonNull).collect(Collectors.toSet());

        Set<Long> modifierTypeIds = menu.getModifierTypes().stream().map(ModifierType::getId)
            .filter(Objects::nonNull).collect(Collectors.toSet());

        return new MenuResponseCached(id, categoryIds, modifierTypeIds);
    }

}
