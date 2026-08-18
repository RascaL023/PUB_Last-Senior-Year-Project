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
import org.springframework.web.bind.annotation.PatchMapping;
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
import id.my.rascal.menu.internal.model.response.MenuResponseCached;
import id.my.rascal.menu.internal.service.MenuV2ApplicationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v2/menus")
public class MenuControllerV2 {

    private final MenuV2ApplicationService menuV2Service;
    private final String DEFAULT_GET_SUCCESS_MESSAGE = "Menu successfully retrieved";
    private final String DEFAULT_CREATE_SUCCESS_MESSAGE = "Menu successfully created";
    private final String DEFAULT_UPDATE_SUCCESS_MESSAGE = "Menu successfully updated";

    public MenuControllerV2(MenuV2ApplicationService menuV2Service) {
        this.menuV2Service = menuV2Service;
    }

    @PostMapping
    public ResponseEntity<SuccessTemplate<MenuResponseCached>> create(@Valid @RequestBody MenuRequest request) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            DEFAULT_CREATE_SUCCESS_MESSAGE,
            menuV2Service.create(request)
        );
    }

    @GetMapping
    public ResponseEntity<SuccessPagedTemplate<List<MenuResponseCached>>> getAll(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Long categoryId,
        @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<MenuResponseCached> page = menuV2Service.getAllPaged(name, categoryId, pageable);

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
    public ResponseEntity<SuccessTemplate<MenuResponseCached>> getById(@PathVariable("id") Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            DEFAULT_GET_SUCCESS_MESSAGE,
            menuV2Service.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessTemplate<MenuResponseCached>> update(
        @PathVariable("id") Long id,
        @Valid @RequestBody MenuPutRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            DEFAULT_UPDATE_SUCCESS_MESSAGE,
            menuV2Service.update(id, request)
        );
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<SuccessTemplate<MenuResponseCached>> restore(
        @PathVariable("id") Long id
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            DEFAULT_UPDATE_SUCCESS_MESSAGE,
            menuV2Service.restore(id)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        menuV2Service.delete(id);

        return ResponseEntity.noContent().build();
    }
    
}