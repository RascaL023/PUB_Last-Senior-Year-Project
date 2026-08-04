package id.my.rascal.menu.internal.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.ConflictException;
import id.my.rascal.common.util.StringUtil;
import id.my.rascal.menu.internal.entity.MenuCategory;
import id.my.rascal.menu.internal.model.request.MenuCategoryPutRequest;
import id.my.rascal.menu.internal.model.request.MenuCategoryRequest;
import id.my.rascal.menu.internal.model.response.MenuCategoryResponse;
import id.my.rascal.menu.internal.repository.MenuCategoryRepository;

@Service
public class MenuCategoryService {

    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuCategoryHelper menuCategoryHelper;

    public MenuCategoryService(
        MenuCategoryRepository menuCategoryRepository,
        MenuCategoryHelper menuCategoryHelper
    ) {
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuCategoryHelper = menuCategoryHelper;
    }

    @Transactional
    public MenuCategoryResponse create(MenuCategoryRequest request) {
        validateDuplicateCode(request.categoryCode());
        MenuCategory menuCategory = fill(
            new MenuCategory(), request.displayName(), 
            request.categoryCode(), request.displayOrder()
        );

        menuCategory.setCreatedAt(LocalDateTime.now());
        return toResponse(menuCategoryRepository.save(menuCategory));
    }

    @Transactional(readOnly = true)
    public MenuCategoryResponse getById(Long id) {
        return toResponse(menuCategoryHelper.getActiveById(id));
    }

    @Transactional(readOnly = true)
    public Page<MenuCategoryResponse> getAllPaged(String displayName, boolean showDeleted, Pageable pageable) {
        return menuCategoryRepository.searchActiveCategories(
            StringUtil.normalizeSearch(displayName), showDeleted, pageable
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> getAllByIds(Iterable<Long> ids) {
        return menuCategoryHelper.getActiveByIds(ids)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public MenuCategoryResponse update(Long id, MenuCategoryPutRequest request) {
        validateDuplicateCode(request.categoryCode());
        MenuCategory menuCategory = assign(menuCategoryHelper.getActiveById(id), request);

        menuCategory.setUpdatedAt(LocalDateTime.now());
        return toResponse(menuCategoryRepository.save(menuCategory));
    }

    @Transactional
    public void delete(Long id) {
        MenuCategory menuCategory = menuCategoryHelper.getActiveById(id);

        menuCategory.setDeletedAt(LocalDateTime.now());
        menuCategoryRepository.save(menuCategory);
    }

    private void validateDuplicateCode(String code) {
        if (menuCategoryRepository.existsByCategoryCode(StringUtil.toSlug(code)))
            throw new ConflictException("Category code already exist");
    }

    private MenuCategory assign(MenuCategory menuCategory, MenuCategoryPutRequest request) {
        String displayName = StringUtil.normalizeSpaces(request.displayName());
        displayName = StringUtil.capitalize(displayName);

        menuCategory.setDisplayName(displayName);
        menuCategory.setCategoryCode(StringUtil.toSlug(request.categoryCode()));
        menuCategory.setDisplayOrder(request.displayOrder());

        return menuCategory;
    }

    private MenuCategory fill(MenuCategory menuCategory, String displayName, String categoryCode, Integer displayOrder) {
        displayName = StringUtil.normalizeSpaces(displayName);
        menuCategory.setDisplayName(StringUtil.capitalize(displayName));
        menuCategory.setCategoryCode(StringUtil.toSlug(categoryCode));
        menuCategory.setDisplayOrder(displayOrder);

        return menuCategory;
    }

    private MenuCategoryResponse toResponse(MenuCategory menuCategory) {
        return new MenuCategoryResponse(
            menuCategory.getId(),
            menuCategory.getDisplayName(),
            menuCategory.getCategoryCode(),
            menuCategory.getDisplayOrder()
        );
    }

}
