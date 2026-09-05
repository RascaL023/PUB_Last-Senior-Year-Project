package id.my.rascal.menu.internal.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import id.my.rascal.image.api.ImageApi;
import id.my.rascal.menu.internal.entity.Menu;
import id.my.rascal.menu.internal.entity.MenuCategory;
import id.my.rascal.menu.internal.entity.ModifierOption;
import id.my.rascal.menu.internal.entity.ModifierType;
import id.my.rascal.menu.internal.model.search.CategoryProjection;
import id.my.rascal.menu.internal.model.search.DeletedScope;
import id.my.rascal.menu.internal.model.search.MenuSearchDocument;
import id.my.rascal.menu.internal.model.search.ModifierOptionProjection;
import id.my.rascal.menu.internal.model.search.ModifierTypeProjection;
import id.my.rascal.menu.internal.repository.MenuRepository;
import id.my.rascal.search.api.SearchApi;
import id.my.rascal.search.api.SearchApiRequest;
import id.my.rascal.search.api.SearchHit;
import id.my.rascal.search.api.SearchResponse;
import id.my.rascal.search.api.SearchUnavailableException;

@Service
public class MenuSearchService {

    private static final Logger log = LoggerFactory.getLogger(MenuSearchService.class);
    private static final String INDEX_NAME = "menus";
    private static final Set<String> SORTABLE_ATTRIBUTES = Set.of("name", "basePrice", "createdAt");
    private final SearchApi searchClient;
    private final ImageApi imageService;
    private final MenuRepository menuRepository;

    public MenuSearchService(
        SearchApi searchClient,
        ImageApi imageService,
        MenuRepository menuRepository
    ) {
        this.searchClient = searchClient;
        this.menuRepository = menuRepository;
        this.imageService = imageService;
    }

    @Transactional(readOnly = true)
    public Page<MenuSearchDocument> searchPaged(
        String query,
        Long categoryId,
        Integer minPrice,
        Integer maxPrice,
        Boolean isAvailable,
        Pageable pageable,
        DeletedScope deletedScope
    ) {
        try {
            return searchWithInfra(query, categoryId, minPrice, maxPrice, isAvailable, pageable, deletedScope);
        } catch (SearchUnavailableException e) {
            log.warn("Search with infrastructure unavailable, falling back to Database like query: {}", e.getMessage());
            return searchWithDatabase(query, categoryId, minPrice, maxPrice, isAvailable, pageable, deletedScope);
        }
    }

    @Transactional(readOnly = true)
    public Page<MenuSearchDocument> searchPaged(
        String query,
        Long categoryId,
        Pageable pageable
    ) {
        return searchPaged(query, categoryId, null, null, null, pageable, DeletedScope.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Optional<MenuSearchDocument> getById(Long id, DeletedScope deletedScope) {
        try {
            Optional<MenuSearchDocument> document = searchClient.getDocument(
                INDEX_NAME, String.valueOf(id), MenuSearchDocument.class
            );
            if (document.isEmpty())
                return Optional.empty();

            return matchesScope(document.get(), deletedScope) ? document : Optional.empty();
        } catch (SearchUnavailableException e) {
            log.warn("Meilisearch unavailable, falling back to PostgreSQL for menu {}: {}", id, e.getMessage());
            return getByIdWithPostgres(id, deletedScope);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMenuIndexEvent(MenuIndexEvent event) {
        upsertForEvent(event.menu());
        log.debug("Indexed menu {} into Meilisearch via event (isDeleted={})",
            event.menu().getId(), event.menu().getDeletedAt() != null);
    }

    public void indexMenu(Menu menu) { upsert(menu); }
    public void updateMenuIndex(Menu menu) { upsert(menu); }
    public void markDeletedInIndex(Menu menu) { upsert(menu); }



    private void upsertForEvent(Menu menu) {
        MenuSearchDocument doc = toSearchDocument(menu);
        searchClient.index(INDEX_NAME, doc);
        log.debug("Indexed menu {} into Meilisearch (isDeleted={})", menu.getId(), doc.getIsDeleted());
    }

    private void upsert(Menu menu) {
        try { upsertForEvent(menu); } 
        catch (SearchUnavailableException e) {
            log.warn("Failed to index menu {} into Meilisearch: {}", menu.getId(), e.getMessage());
        }
    }

    private Page<MenuSearchDocument> searchWithInfra(
        String query,
        Long categoryId,
        Integer minPrice,
        Integer maxPrice,
        Boolean isAvailable,
        Pageable pageable,
        DeletedScope deletedScope
    ) {
        List<String> filter = buildFilter(categoryId, minPrice, maxPrice, isAvailable, deletedScope);
        List<String> sort = buildSort(pageable);

        int page = pageable.getPageNumber() + 1;
        int pageSize = pageable.getPageSize();

        SearchApiRequest request = new SearchApiRequest(
            query != null ? query : "",
            filter, sort,
            page, pageSize
        );

        SearchResponse<MenuSearchDocument> response = searchClient.search(INDEX_NAME, request, MenuSearchDocument.class);
        List<MenuSearchDocument> docs = response.hits().stream()
            .map(SearchHit::document)
            .collect(Collectors.toList());

        return new PageImpl<>(docs, pageable, response.totalHits());
    }


    private Page<MenuSearchDocument> searchWithDatabase(
        String query,
        Long categoryId,
        Integer minPrice,
        Integer maxPrice,
        Boolean isAvailable,
        Pageable pageable,
        DeletedScope deletedScope
    ) {
        Page<Long> idPage = menuRepository.findSearchIdsForScope(
            query, categoryId,
            minPrice, maxPrice,
            isAvailable, toShowDeleted(deletedScope),
            pageable
        );

        if (idPage.getContent().isEmpty())
            return Page.empty(pageable);

        List<Menu> menus = menuRepository.findAllByIds(idPage.getContent());
        Map<Long, Menu> byId = menus.stream()
            .collect(Collectors.toMap(Menu::getId, m -> m));

        List<MenuSearchDocument> docs = idPage.getContent().stream()
            .map(byId::get)
            .filter(Objects::nonNull)
            .map(this::toSearchDocument)
            .collect(Collectors.toList());

        return new PageImpl<>(docs, pageable, idPage.getTotalElements());
    }

    private Optional<MenuSearchDocument> getByIdWithPostgres(Long id, DeletedScope deletedScope) {
        Optional<Menu> menu = switch (deletedScope) {
            case ACTIVE -> menuRepository.findWithRelationsById(id, false);
            case DELETED -> menuRepository.findWithRelationsById(id, true);
            case ALL -> menuRepository.findAnyWithRelationsById(id);
        };

        return menu.map(this::toSearchDocument);
    }


    private List<String> buildFilter(
        Long categoryId,
        Integer minPrice,
        Integer maxPrice,
        Boolean isAvailable,
        DeletedScope deletedScope
    ) {
        List<String> filter = new ArrayList<>();

        switch (deletedScope) {
            case ACTIVE -> filter.add("isDeleted = false");
            case DELETED -> filter.add("isDeleted = true");
            case ALL -> {}
        }

        if (categoryId != null) filter.add("categoryIds = " + categoryId);
        if (minPrice != null) filter.add("basePrice >= " + minPrice);
        if (maxPrice != null) filter.add("basePrice <= " + maxPrice);
        if (isAvailable != null) filter.add("isAvailable = " + isAvailable);

        return filter;
    }

    private List<String> buildSort(Pageable pageable) {
        if (pageable.getSort() == null || pageable.getSort().isUnsorted())
            return List.of();

        return pageable.getSort().stream()
            .filter(order -> SORTABLE_ATTRIBUTES.contains(order.getProperty()))
            .map(order -> order.getProperty() + ":" + order.getDirection().name().toLowerCase())
            .toList();
    }

    private Boolean toShowDeleted(DeletedScope deletedScope) {
        return switch (deletedScope) {
            case ACTIVE -> Boolean.FALSE;
            case DELETED -> Boolean.TRUE;
            case ALL -> null;
        };
    }

    private boolean matchesScope(MenuSearchDocument document, DeletedScope deletedScope) {
        boolean deleted = Boolean.TRUE.equals(document.getIsDeleted());
        return switch (deletedScope) {
            case ACTIVE -> !deleted;
            case DELETED -> deleted;
            case ALL -> true;
        };
    }


    private MenuSearchDocument toSearchDocument(Menu menu) {
        List<MenuCategory> categories = menu.getCategories() != null ? menu.getCategories() : List.of();
        List<ModifierType> modifierTypes = menu.getModifierTypes() != null ? menu.getModifierTypes() : List.of();
        List<String> imageUrls = menu.getImageUrls() != null
            ? menu.getImageUrls().stream().map(imageService::resolveUrl).toList()
            : List.of();

        return new MenuSearchDocument(
            menu.getId(),
            menu.getName(),
            menu.getDescription(),
            menu.getBasePrice(),
            menu.getIsAvailable(),
            menu.getDeletedAt() != null,
            menu.getDeletedAt(),
            menu.getCreatedAt(),
            menu.getUpdatedAt(),
            imageUrls,
            categories.stream().map(MenuCategory::getId).toList(),
            categories.stream().map(this::toCategoryProjection).toList(),
            modifierTypes.stream().map(this::toModifierTypeProjection).toList()
        );
    }

    private CategoryProjection toCategoryProjection(MenuCategory category) {
        return new CategoryProjection(
            category.getId(),
            category.getDisplayName(),
            category.getCategoryCode(),
            category.getDisplayOrder()
        );
    }

    private ModifierTypeProjection toModifierTypeProjection(ModifierType modifierType) {
        List<ModifierOption> options = modifierType.getModifierOptions() != null
            ? modifierType.getModifierOptions()
            : List.of();

        return new ModifierTypeProjection(
            modifierType.getId(),
            modifierType.getName(),
            modifierType.getMinSelection(),
            modifierType.getMaxSelection(),
            options.stream().map(this::toModifierOptionProjection).toList()
        );
    }

    private ModifierOptionProjection toModifierOptionProjection(ModifierOption option) {
        return new ModifierOptionProjection(
            option.getId(),
            option.getName(),
            option.getAdditionalPrice()
        );
    }

}
