package id.my.rascal.menu.internal.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.invoice.api.InvoiceReportApi;
import id.my.rascal.menu.internal.entity.Menu;
import id.my.rascal.menu.internal.model.request.MenuPutRequest;
import id.my.rascal.menu.internal.model.request.MenuRequest;
import id.my.rascal.menu.internal.model.response.MenuResponse;
import id.my.rascal.menu.internal.model.search.DeletedScope;
import id.my.rascal.menu.internal.model.search.MenuSearchDocument;
import id.my.rascal.menu.internal.repository.MenuRepository;

@Service
public class MenuV1ApplicationService {

    private static final int TOP_MENU_MAX_ROWS = 100;

    private final MenuService menuService;
    private final MenuSearchService menuSearchService;
    private final MenuResponseMapper menuResponseMapper;
    private final MenuRepository menuRepository;
    private final InvoiceReportApi invoiceReportApi;

    public MenuV1ApplicationService(
        MenuService menuService,
        MenuSearchService menuSearchService,
        MenuResponseMapper menuResponseMapper,
        MenuRepository menuRepository,
        InvoiceReportApi invoiceReportApi
    ) {
        this.menuService = menuService;
        this.menuSearchService = menuSearchService;
        this.menuResponseMapper = menuResponseMapper;
        this.menuRepository = menuRepository;
        this.invoiceReportApi = invoiceReportApi;
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

    @Transactional(readOnly = true)
    public Page<MenuResponse> getTopMenusPaged(int days, Pageable pageable) {
        LocalDateTime end = LocalDateTime.now(ZoneId.systemDefault());
        LocalDateTime start = end.minusDays(Math.max(days, 1));

        List<Long> orderedIds = invoiceReportApi.topMenuSales(start, end, TOP_MENU_MAX_ROWS).stream()
            .map(InvoiceReportApi.MenuSalesRow::menuId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        if (orderedIds.isEmpty()) return Page.empty(pageable);

        Map<Long, Menu> byId = menuRepository.findAllByIds(orderedIds).stream()
            .collect(Collectors.toMap(Menu::getId, menu -> menu));

        List<MenuResponse> ordered = orderedIds.stream()
            .map(byId::get)
            .filter(Objects::nonNull)
            .map(menuResponseMapper::from)
            .toList();

        int total = ordered.size();
        int from = (int) Math.min(pageable.getOffset(), total);
        int to = Math.min(from + pageable.getPageSize(), total);

        return new PageImpl<>(ordered.subList(from, to), pageable, total);
    }

    @Transactional
    public MenuResponse update(Long id, MenuPutRequest request) {
        return menuResponseMapper.from(menuService.update(id, request));
    }

    @Transactional
    public MenuResponse restore(Long id) { 
        return menuResponseMapper.from(menuService.restore(id)); 
    }

    @Transactional
    public void delete(Long id) { menuService.delete(id); }

}
