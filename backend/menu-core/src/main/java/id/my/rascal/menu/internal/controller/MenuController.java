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
import id.my.rascal.menu.internal.model.request.MenuPutRequest;
import id.my.rascal.menu.internal.model.request.MenuRequest;
import id.my.rascal.menu.internal.model.response.MenuResponse;
import id.my.rascal.menu.internal.service.MenuService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/menus")
public class MenuController {

    private final MenuService menuService;
    private final String DEFAULT_GET_SUCCESS_MESSAGE = "Menu successfully retrieved";
    private final String DEFAULT_CREATE_SUCCESS_MESSAGE = "Menu successfully created";
    private final String DEFAULT_UPDATE_SUCCESS_MESSAGE = "Menu successfully updated";

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @PostMapping
    public ResponseEntity<SuccessTemplate<MenuResponse>> create(@Valid @RequestBody MenuRequest request) {
        return ApiResponse.success(
            HttpStatus.CREATED, 
            DEFAULT_CREATE_SUCCESS_MESSAGE,
            menuService.create(request)
        );
    }

    @GetMapping
    public ResponseEntity<SuccessPagedTemplate<List<MenuResponse>>> getAll(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Long categoryId,
        @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<MenuResponse> page = menuService.getAllPaged(name, categoryId, pageable);

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
    public ResponseEntity<SuccessTemplate<MenuResponse>> getById(@PathVariable("id") Long id) {
        return ApiResponse.success(
            HttpStatus.OK, 
            DEFAULT_GET_SUCCESS_MESSAGE,
            menuService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessTemplate<MenuResponse>> update(
        @PathVariable("id") Long id,
        @Valid @RequestBody MenuPutRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK, 
            DEFAULT_UPDATE_SUCCESS_MESSAGE,
            menuService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        menuService.delete(id);

        return ResponseEntity.noContent().build();
    }
    
}
