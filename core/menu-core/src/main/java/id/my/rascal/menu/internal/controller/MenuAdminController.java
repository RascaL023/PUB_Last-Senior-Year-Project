package id.my.rascal.menu.internal.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.template.SuccessPagedTemplate;
import id.my.rascal.common.template.SuccessTemplate;
import id.my.rascal.menu.internal.model.response.MenuResponse;
import id.my.rascal.menu.internal.model.search.DeletedScope;
import id.my.rascal.menu.internal.service.MenuAdminApplicationService;
import jakarta.validation.constraints.Min;

/**
 * Admin menu read endpoints. Search/detail include soft-deleted menus and expose
 * {@code deletedAt} when applicable.
 * <p>
 * NOTE (security, Q3): {@code @PreAuthorize} is intentionally left as a commented
 * placeholder — authorization is a separate concern and will be activated later:
 *
 * <pre>
 * // @PreAuthorize("hasAnyAuthority('menu.read', 'menu.*')")
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/admin/menus")
public class MenuAdminController {

    private final MenuAdminApplicationService menuAdminService;
    private final String DEFAULT_GET_SUCCESS_MESSAGE = "Menu successfully retrieved";

    public MenuAdminController(MenuAdminApplicationService menuAdminService) {
        this.menuAdminService = menuAdminService;
    }

    // @PreAuthorize("hasAnyAuthority('menu.read', 'menu.*')")
    @GetMapping("/search")
    public ResponseEntity<SuccessPagedTemplate<List<MenuResponse>>> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) @Min(0) Long categoryId,
        @RequestParam(required = false) @Min(0) Integer minPrice,
        @RequestParam(required = false) @Min(0) Integer maxPrice,
        @RequestParam(required = false) Boolean isAvailable,
        @RequestParam(required = false, defaultValue = "active") String deleted,
        @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<MenuResponse> page = menuAdminService.searchPaged(
            name,
            categoryId,
            minPrice,
            maxPrice,
            isAvailable,
            DeletedScope.from(deleted),
            pageable
        );

        return ApiResponse.paged(
            HttpStatus.OK,
            DEFAULT_GET_SUCCESS_MESSAGE,
            page.getContent(),
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    // @PreAuthorize("hasAnyAuthority('menu.read', 'menu.*')")
    @GetMapping("/{id}")
    public ResponseEntity<SuccessTemplate<MenuResponse>> getById(@PathVariable("id") Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            DEFAULT_GET_SUCCESS_MESSAGE,
            menuAdminService.getById(id)
        );
    }

}
