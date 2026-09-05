package id.my.rascal.menu.internal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.menu.internal.model.response.MenuResponse;
import id.my.rascal.menu.internal.model.search.DeletedScope;
import id.my.rascal.menu.internal.model.search.MenuSearchDocument;

@Service
public class MenuAdminApplicationService {

    private final MenuSearchService menuSearchService;
    private final MenuResponseMapper menuResponseMapper;

    public MenuAdminApplicationService(
        MenuSearchService menuSearchService,
        MenuResponseMapper menuResponseMapper
    ) {
        this.menuSearchService = menuSearchService;
        this.menuResponseMapper = menuResponseMapper;
    }

    @Transactional(readOnly = true)
    public Page<MenuResponse> searchPaged(
        String query,
        Long categoryId,
        Integer minPrice,
        Integer maxPrice,
        Boolean isAvailable,
        DeletedScope deletedScope,
        Pageable pageable
    ) {
        Page<MenuSearchDocument> results = menuSearchService.searchPaged(
            query, categoryId, minPrice, maxPrice, isAvailable, pageable, deletedScope
        );

        return new PageImpl<>(
            results.getContent().stream()
                .map(menuResponseMapper::from)
                .toList(),
            pageable,
            results.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public MenuResponse getById(Long id) {
        return menuSearchService.getById(id, DeletedScope.ALL)
            .map(menuResponseMapper::from)
            .orElseThrow(() -> new NotFoundException("Menu with id " + id + " not found"));
    }

}
