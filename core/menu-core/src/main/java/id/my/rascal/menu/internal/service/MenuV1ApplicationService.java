package id.my.rascal.menu.internal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.menu.internal.model.request.MenuPutRequest;
import id.my.rascal.menu.internal.model.request.MenuRequest;
import id.my.rascal.menu.internal.model.response.MenuResponse;
import id.my.rascal.menu.internal.model.search.DeletedScope;
import id.my.rascal.menu.internal.model.search.MenuSearchDocument;

@Service
public class MenuV1ApplicationService {

    private final MenuService menuService;
    private final MenuSearchService menuSearchService;
    private final MenuResponseMapper menuResponseMapper;

    public MenuV1ApplicationService(
        MenuService menuService,
        MenuSearchService menuSearchService,
        MenuResponseMapper menuResponseMapper
    ) {
        this.menuService = menuService;
        this.menuSearchService = menuSearchService;
        this.menuResponseMapper = menuResponseMapper;
    }

    @Transactional
    public MenuResponse create(MenuRequest request) {
        return menuResponseMapper.from(menuService.create(request));
    }

    public MenuResponse getById(Long id) {
        return menuSearchService.getById(id, DeletedScope.ACTIVE)
            .map(menuResponseMapper::from)
            .orElseThrow(() -> new NotFoundException("Menu with id " + id + " not found"));
    }

    public Page<MenuResponse> getAllPaged(
        String name,
        Long categoryId,
        Integer minPrice,
        Integer maxPrice,
        Pageable pageable
    ) {
        Page<MenuSearchDocument> searchResults = menuSearchService.searchPaged(
            name, categoryId, 
            minPrice, maxPrice, 
            null, pageable, 
            DeletedScope.ACTIVE
        );

        return new PageImpl<>(
            searchResults.getContent().stream()
                .map(menuResponseMapper::from)
                .toList(),
            pageable,
            searchResults.getTotalElements()
        );
    }

    @Transactional
    public MenuResponse update(Long id, MenuPutRequest request) {
        return menuResponseMapper.from(menuService.update(id, request));
    }

    @Transactional
    public MenuResponse restore(Long id) { return menuResponseMapper.from(menuService.restore(id)); }

    @Transactional
    public void delete(Long id) { menuService.delete(id); }

}
