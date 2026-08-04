package id.my.rascal.menu.internal.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.template.SuccessPagedTemplate;
import id.my.rascal.common.template.SuccessTemplate;
import id.my.rascal.menu.internal.model.request.MenuCategoryPutRequest;
import id.my.rascal.menu.internal.model.request.MenuCategoryRequest;
import id.my.rascal.menu.internal.model.response.MenuCategoryResponse;
import id.my.rascal.menu.internal.service.MenuCategoryService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/menu-categories")
public class MenuCategoryController {

    private final MenuCategoryService menuCategoryService;
    private final String DEFAULT_GET_SUCCESS_MESSAGE = "Menu successfully retrieved";
    private final String DEFAULT_CREATE_SUCCESS_MESSAGE = "Menu successfully created";
    private final String DEFAULT_UPDATE_SUCCESS_MESSAGE = "Menu successfully updated";

    public MenuCategoryController(MenuCategoryService menuCategoryService) {
        this.menuCategoryService = menuCategoryService;
    }

    @PostMapping
    public ResponseEntity<SuccessTemplate<MenuCategoryResponse>> create(@Valid @RequestBody MenuCategoryRequest request) {
        return ApiResponse.success(
            HttpStatus.CREATED, 
            DEFAULT_CREATE_SUCCESS_MESSAGE,
            menuCategoryService.create(request)
        );
    }

    @GetMapping
    public ResponseEntity<SuccessPagedTemplate<List<MenuCategoryResponse>>> getAll(
        @RequestParam(required = false) String name,
        @PageableDefault(size = 10, sort = "displayName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<MenuCategoryResponse> page = menuCategoryService.getAllPaged(name, false, pageable);

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

    @GetMapping("/{id}")
    public ResponseEntity<SuccessTemplate<MenuCategoryResponse>> getById(@PathVariable("id") Long id) {
        return ApiResponse.success(
            HttpStatus.OK, 
            DEFAULT_GET_SUCCESS_MESSAGE,
            menuCategoryService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessTemplate<MenuCategoryResponse>> update(
        @PathVariable("id") Long id,
        @Valid @RequestBody MenuCategoryPutRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK, 
            DEFAULT_UPDATE_SUCCESS_MESSAGE,
            menuCategoryService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        menuCategoryService.delete(id);

        return ResponseEntity.noContent().build();
    }
    
}
